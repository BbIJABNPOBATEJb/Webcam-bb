package me.bbijabnpobatejb.webcam.client.handlers;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import me.bbijabnpobatejb.webcam.WebcamMod;
import me.bbijabnpobatejb.webcam.client.WebcamClient;
import me.bbijabnpobatejb.webcam.client.config.ConfigMenu;
import me.bbijabnpobatejb.webcam.client.object.PlayerVideo;
import me.bbijabnpobatejb.webcam.compat.flashback.WebcamFlashback;
import me.bbijabnpobatejb.webcam.message.VideoFramePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static me.bbijabnpobatejb.webcam.WebcamMod.isFlashbackPresent;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CodecThread {

    volatile boolean running = false;
    public PlayerVideo clientPlayerVideo;
    ExecutorService cameraLoopExecutor;
    final CameraManager cameraManager = new CameraManager();

    public void start() {
        if (running) {
            WebcamMod.LOGGER.warn("Camera loop is already running.");
            return;
        }

        cameraLoopExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "Camera-Capture-Thread"));
        running = true;

        assert MinecraftClient.getInstance().player != null;
        clientPlayerVideo = new PlayerVideo(ConfigMenu.renderWidth, ConfigMenu.renderHeight, MinecraftClient.getInstance().player.getUuid());

        cameraLoopExecutor.submit(() -> {
            try {
                cameraManager.start();
            } catch (Exception e) {
                WebcamMod.LOGGER.error("Failed to initialize camera. Stopping loop.", e);
                running = false;
            }

            WebcamMod.LOGGER.info("Camera loop started.");
            long lastFrameTime = System.currentTimeMillis();

            while (running) {
                try {
                    long frameDurationMillis = 1000 / Math.max(1, ConfigMenu.fps);
                    long currentTime = System.currentTimeMillis();

                    if (currentTime - lastFrameTime >= frameDurationMillis) {
                        lastFrameTime = currentTime;
                        loop();
                    } else {
                        Thread.sleep(1);
                    }
                } catch (InterruptedException e) {
                    WebcamMod.LOGGER.info("Camera loop thread interrupted.");
                    running = false;
                } catch (Exception e) {
                    WebcamMod.LOGGER.error("An error occurred in the camera loop.", e);
                }
            }

            WebcamMod.LOGGER.info("Camera loop stopped.");
            cameraManager.stop();
        });
    }


    public void stop() {
        running = false;
        if (cameraLoopExecutor != null && !cameraLoopExecutor.isShutdown()) {
            cameraLoopExecutor.shutdown();
        }
    }


    public void loop() {
        try {
            if (cameraManager.getCurrentWebcam() == null) return;

            cameraManager.get(clientPlayerVideo);
            WebcamClient.getInstance().getPlayerFeeds().update(clientPlayerVideo);

            if (isFlashbackPresent()) {
                WebcamFlashback.submitFrame(clientPlayerVideo);
            }

            if (ClientPlayNetworking.canSend(VideoFramePayload.ID)) {
                ClientPlayNetworking.send(new VideoFramePayload(clientPlayerVideo));
            } else {
                WebcamMod.LOGGER.warn("Could not send video frame, network handler is null?");
            }
        } catch (IOException e) {
            WebcamMod.LOGGER.error("Could not get image from webcam", e);
        }
    }

    void stopThread() {
        running = false;
    }
}
