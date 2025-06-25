package me.bbijabnpobatejb.webcam.client.object;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlayerVideo {
    int width;
    int height;
    UUID uuid;
    byte[] frame;


    public PlayerVideo(int width, int height, UUID uuid) {
        this.width = width;
        this.height = height;
        this.uuid = uuid;
    }

    public ByteBuffer getBuffer() {
        BufferedImage image = null;
        try {
            image = ImageIO.read(new ByteArrayInputStream(this.frame));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int width = image.getWidth();
        int height = image.getHeight();

        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 3);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);

                byte r = (byte) ((pixel >> 16) & 0xFF);
                byte g = (byte) ((pixel >> 8) & 0xFF);
                byte b = (byte) (pixel & 0xFF);

                buffer.put(r);
                buffer.put(g);
                buffer.put(b);
            }
        }

        buffer.flip();
        return buffer;
    }

    public void setSize(Dimension viewSize) {
        this.width = viewSize.width;
        this.height = viewSize.height;
    }
}