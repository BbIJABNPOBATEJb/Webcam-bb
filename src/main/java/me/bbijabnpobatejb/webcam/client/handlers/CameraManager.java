package me.bbijabnpobatejb.webcam.client.handlers;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import me.bbijabnpobatejb.webcam.WebcamMod;
import me.bbijabnpobatejb.webcam.client.WebcamClient;
import me.bbijabnpobatejb.webcam.client.config.ConfigMenu;
import me.bbijabnpobatejb.webcam.client.object.PlayerVideo;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

import static me.bbijabnpobatejb.webcam.WebcamMod.showToastText;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class CameraManager {


    final Predicate<String> IGNORE_CAMERA = s -> s.contains("Virtual");

    volatile Webcam webcam;
    String lastCamera = "";

    public CameraManager() {
    }

    public void setWebcam(Webcam webcam) {
        this.webcam = webcam;
        lastCamera = webcam.getName();
        assert MinecraftClient.getInstance().player != null;

        WebcamClient.getInstance().getCodecThread().clientPlayerVideo.setSize(webcam.getViewSize());
    }


    public void start() {
        List<Webcam> webcams = Webcam.getWebcams();
        if (webcams.isEmpty()) {
            WebcamMod.LOGGER.error("No webcams found.");
            showToastText("webcam_error.title", "webcam_error.no_webcams_available");
        }
        WebcamMod.LOGGER.info("List webcams:");
        for (Webcam wc : webcams) {

            WebcamMod.LOGGER.info("- {} {}", wc.getName(), wc.getViewSize().toString());
            for (Dimension dimension : wc.getViewSizes()) {
                WebcamMod.LOGGER.info(dimension.toString());
            }
            for (Dimension dimension : wc.getCustomViewSizes()) {
                WebcamMod.LOGGER.info(dimension.toString());
            }

        }
        if (!lastCamera.isEmpty()) {
            for (Webcam w : webcams) {
                if (w.getName().equals(lastCamera)) {
                    if (tryOpenCamera(w)) return;
                }
            }
        }
        for (Webcam wc : webcams) {
            if (IGNORE_CAMERA.test(wc.getName())) continue;
            if (tryOpenCamera(wc)) return;
        }
        showToastText("webcam_error.title", "webcam_error.webcams_are_busy");
    }

    boolean tryOpenCamera(Webcam w) {
        return tryOpenCamera(w, null);
    }

    boolean tryOpenCamera(Webcam w, @Nullable Dimension size) {
        try {
            if (size != null) {
                w.setViewSize(size);
            }
            w.open();
            setWebcam(w);
            WebcamMod.LOGGER.info("Successfully initialized webcam: {}", w.getName());
            return true;
        } catch (WebcamException e) {
            WebcamMod.LOGGER.warn("Failed to open webcam '{}'. Trying next one...", w.getName());
            w.close();
        }
        return false;
    }


    public void stop() {
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
            WebcamMod.LOGGER.info("Webcam {} closed.", webcam.getName());
        }
    }

    public List<String> getWebcamList() {
        return Webcam.getWebcams().stream()
                .map(Webcam::getName)
                .filter(IGNORE_CAMERA.negate())
                .toList();
    }


    public void setWebcamByName(String name) {

        Webcam newWebcam = Webcam.getWebcamByName(name);
        if (newWebcam == null) {
            throw new WebcamException("Webcam with name \"" + name + "\" not found.");
        }
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }


        setWebcam(newWebcam);
        WebcamMod.LOGGER.info("Successfully switched to webcam: {}", newWebcam.getName());

    }

    public String getCurrentWebcamName() {
        return (webcam != null && webcam.isOpen()) ? webcam.getName() : "None";
    }

    public Webcam getCurrentWebcam() {
        return (webcam != null && webcam.isOpen()) ? webcam : null;
    }


    public void get(PlayerVideo playerVideo) throws IOException {
        if (webcam == null || !webcam.isOpen()) {
            return;
        }

        BufferedImage image = webcam.getImage();
        if (image == null) {
            return;
        }

        BufferedImage resizedImage = resize(image, playerVideo.getWidth(), playerVideo.getHeight());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {

            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            writer.setOutput(ios);

            JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
            jpegParams.setCompressionMode(JPEGImageWriteParam.MODE_EXPLICIT);
            float compression = ConfigMenu.compression / 100f;
            if (compression < 1) {
                jpegParams.setCompressionQuality(1 - compression);
            }

            writer.write(null, new IIOImage(resizedImage, null, null), jpegParams);
            writer.dispose();
            playerVideo.setFrame(baos.toByteArray());
        }
    }


    BufferedImage resize(BufferedImage original, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }

    public void switchCamera() {
        ConfigMenu.renderEnabled = !ConfigMenu.renderEnabled;
        updateStateCamera();

        String s = ConfigMenu.renderEnabled ? "enable" : "disable";
        showToastText("webcam_notify.title", "webcam_notify." + s);
    }

    public void updateStateCamera() {
        if (ConfigMenu.renderEnabled) {
            start();
        } else {
            stop();
        }
    }

    public void setViewSize(Dimension viewSize) {
        if (webcam == null) {
            return;
        }
        stop();
        tryOpenCamera(webcam, viewSize);
    }
}