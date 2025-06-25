package me.bbijabnpobatejb.webcam.client.render.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.isxander.yacl3.gui.image.ImageRenderer;
import lombok.val;
import me.bbijabnpobatejb.webcam.client.WebcamClient;
import me.bbijabnpobatejb.webcam.client.config.ConfigMenu;
import me.bbijabnpobatejb.webcam.client.object.RenderImage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;

public class HudRenderer implements ImageRenderer {

    @Override
    public int render(DrawContext drawContext, int x, int y, int renderWidth, float tickDelta) {
        val webcam = WebcamClient.getInstance().getCameraManager().getCurrentWebcam();
        if (webcam == null) return 0;

        renderWidth = (int) (renderWidth * .8f);

        renderWebcamOnOverlay(drawContext, x, y, renderWidth, renderWidth);
        return renderWidth;
    }

    @Override
    public void close() {
    }


    public void renderWebcamOnHud(DrawContext drawContext) {

        val webcam = WebcamClient.getInstance().getCameraManager().getCurrentWebcam();
        if (webcam == null) return;

        final int screenWidth = drawContext.getScaledWindowWidth();
        final int screenHeight = drawContext.getScaledWindowHeight();


        float size = webcam.getViewSize().width * ConfigMenu.windowScale * .5f;
        float offsetX = ConfigMenu.offsetX;
        float offsetY = ConfigMenu.offsetY;
        float x = 0;
        float y = 0;

        switch (ConfigMenu.renderCorner) {
            case TOP_LEFT -> {
                x = offsetX;
                y = offsetY;
            }
            case TOP_RIGHT -> {
                x = screenWidth - offsetX - size;
                y = offsetY;
            }
            case BOTTOM_LEFT -> {
                x = offsetX;
                y = screenHeight - offsetY - size;
            }
            case BOTTOM_RIGHT -> {
                x = screenWidth - offsetX - size;
                y = screenHeight - offsetY - size;
            }
        }
        renderWebcamOnOverlay(drawContext, x, y, size, size);
    }

    void renderWebcamOnOverlay(DrawContext drawContext, float x, float y, float width, float height) {

        if (!MinecraftClient.isHudEnabled()) return;
        if (!ConfigMenu.previewEnabled) return;
        if (!ConfigMenu.renderEnabled) return;

        val player = MinecraftClient.getInstance().player;
        if (player == null) return;
        RenderImage image = WebcamClient.getInstance().getPlayerFeeds().get(player.getUuid());
        if (image == null || image.data() == null) {
            return;
        }

        updateGpuTexture(image);


        renderWebcamOnOverlay(drawContext, image, x, y, width, height);
    }


    void updateGpuTexture(RenderImage image) {
        image.init();

        glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);

        image.buffer.writeAndSwap(image.data().duplicate());

        glBindTexture(GL_TEXTURE_2D, image.id);
        image.buffer.bind();

        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image.width, image.height, GL_RGB, GL_UNSIGNED_BYTE, 0);
        image.buffer.unbind();

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    void renderWebcamOnOverlay(DrawContext drawContext, RenderImage image, float x, float y, float width, float height) {

        float cutLeft = 0.0f;
        float cutRight = 1.0f;
        float cutTop = 1.0f;
        float cutBottom = 0.0f;

        val webcam = WebcamClient.getInstance().getCameraManager().getCurrentWebcam();
        if (webcam == null) return;

        float cameraWidth = (float) webcam.getViewSize().getWidth();
        float cameraHeight = (float) webcam.getViewSize().getHeight();

        if (cameraWidth > cameraHeight) {
            float ratio = cameraHeight / cameraWidth;
            float excess = (1.0f - ratio) / 2.0f;
            cutLeft = excess;
            cutRight = 1.0f - excess;
        } else if (cameraHeight > cameraWidth) {
            float ratio = cameraWidth / cameraHeight;
            float excess = (1.0f - ratio) / 2.0f;
            cutBottom = excess;
            cutTop = 1.0f - excess;
        }

        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, image.id);

        val matrices = drawContext.getMatrices();
        matrices.push();


        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer;

        switch (ConfigMenu.previewShapeMode) {
            case SQUARE -> {
                buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

                buffer.vertex(x, y + height, 0).texture(cutRight, cutTop);
                buffer.vertex(x + width, y + height, 0).texture(cutLeft, cutTop);
                buffer.vertex(x + width, y, 0).texture(cutLeft, cutBottom);
                buffer.vertex(x, y, 0).texture(cutRight, cutBottom);

                BufferRenderer.drawWithGlobalProgram(buffer.end());
            }
            case CIRCLE -> {
                float cx = x + width / 2f, cy = y + height / 2f, r = width / 2f;
                float uc = (cutLeft + cutRight) / 2f, vc = (cutTop + cutBottom) / 2f;
                float ur = (cutRight - cutLeft) / 2f, vr = (cutTop - cutBottom) / 2f;

                RenderSystem.disableCull();
                RenderSystem.setShader(GameRenderer::getPositionTexProgram);
                RenderSystem.setShaderTexture(0, image.id);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

                Matrix4f m = drawContext.getMatrices().peek().getPositionMatrix();
                buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE);

                for (int i = 0; i < 64; i++) {
                    float a1 = (float) (2 * Math.PI * i / 64), a2 = (float) (2 * Math.PI * (i + 1) / 64);
                    float dx1 = (float) Math.cos(a1), dy1 = (float) Math.sin(a1);
                    float dx2 = (float) Math.cos(a2), dy2 = (float) Math.sin(a2);

                    buffer.vertex(m, cx, cy, 0).texture(uc, vc);
                    buffer.vertex(m, cx + dx1 * r, cy + dy1 * r, 0).texture(uc - dx1 * ur, vc + dy1 * vr);
                    buffer.vertex(m, cx + dx2 * r, cy + dy2 * r, 0).texture(uc - dx2 * ur, vc + dy2 * vr);
                }

                BufferRenderer.drawWithGlobalProgram(buffer.end());
                RenderSystem.enableCull();
            }
        }


        matrices.pop();
        RenderSystem.enableDepthTest();
    }


}