package me.bbijabnpobatejb.webcam.client.object;


import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL33.*;

public class RenderImage {
    public ByteBuffer data;
    public int width;
    public int height;
    public int id;
    public DoublePBO buffer;

    public void init() {
        if (id == 0) {
            id = glGenTextures();
            buffer = new DoublePBO(this.width * this.height * 3);
        }

        glBindTexture(GL_TEXTURE_2D, this.id);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, this.width, this.height, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void fill(PlayerVideo video) {
        this.width = video.getWidth();
        this.height = video.getHeight();
        this.data = video.getBuffer();
    }

    public ByteBuffer data() {
        return this.data;
    }
}