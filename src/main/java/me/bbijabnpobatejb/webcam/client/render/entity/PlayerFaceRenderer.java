package me.bbijabnpobatejb.webcam.client.render.entity;

import com.mojang.blaze3d.systems.RenderSystem;
import me.bbijabnpobatejb.webcam.client.WebcamClient;
import me.bbijabnpobatejb.webcam.client.config.ConfigMenu;
import me.bbijabnpobatejb.webcam.client.object.RenderImage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import static me.bbijabnpobatejb.webcam.client.config.ConfigMenu.*;
import static org.lwjgl.opengl.GL11.*;

public class PlayerFaceRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    public PlayerFaceRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (!playerRenderEnabled) return;

        if (playerRotationMode != ConfigMenu.RotationMode.ON_HEAD) return;

        ClientPlayNetworkHandler clientPlayNetworkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (clientPlayNetworkHandler == null) {
            return;
        }


        RenderImage image = WebcamClient.getInstance().getPlayerFeeds().get(entity.getUuid());
        if (image == null || image.data() == null) {
            return;
        }

        matrixStack.push();

        ModelPart head = getContextModel().head;
        head.rotate(matrixStack);

        matrixStack.translate(0, 0, -0.30);
        matrixStack.scale(0.25f, 0.5f, 1f);
        if (playerShapeMode == ShapeMode.CIRCLE) {
            matrixStack.scale(-1f, .5f, 1f);
            matrixStack.translate(0, -1f, 0);
        }
        matrixStack.translate(playerOffsetX, -playerOffsetY, playerOffsetZ);
        matrixStack.scale(playerRenderScale, playerRenderScale, 1f);



        float widthCamera = image.width;
        float heightCamera = image.height;

        float cutLeft = 0.0f;
        float cutRight = 1.0f;
        float cutTop = 1.0f;
        float cutBottom = 0.0f;

        if (widthCamera > heightCamera) {
            float ratio = heightCamera / widthCamera;
            float excess = (1.0f - ratio) / 2.0f;
            cutLeft = excess;
            cutRight = 1.0f - excess;
        } else if (heightCamera > widthCamera) {
            float ratio = widthCamera / heightCamera;
            float excess = (1.0f - ratio) / 2.0f;
            cutBottom = excess;
            cutTop = 1.0f - excess;
        }


        MatrixStack.Entry entry = matrixStack.peek();
        Matrix4f position = new Matrix4f(entry.getPositionMatrix());
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = null;
        switch (playerShapeMode) {
            case SQUARE -> {
                buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE);
                buffer.vertex(position, 1, -1, 0).texture(cutLeft, cutBottom);
                buffer.vertex(position, 1, 0, 0).texture(cutLeft, cutTop);
                buffer.vertex(position, -1, 0, 0).texture(cutRight, cutTop);

                buffer.vertex(position, -1, 0, 0).texture(cutRight, cutTop);
                buffer.vertex(position, 1, -1, 0).texture(cutLeft, cutBottom);
                buffer.vertex(position, -1, -1, 0).texture(cutRight, cutBottom);
            }
            case CIRCLE -> {
                buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE);

                int segments = 32;

                float uc = (cutLeft + cutRight) / 2f;
                float vc = (cutBottom + cutTop) / 2f;

                for (int i = 0; i < segments; i++) {
                    float angle1 = (float) (i * 2 * Math.PI / segments);
                    float angle2 = (float) ((i + 1) * 2 * Math.PI / segments);

                    float x1 = (float) Math.cos(angle1);
                    float y1 = (float) Math.sin(angle1);

                    float x2 = (float) Math.cos(angle2);
                    float y2 = (float) Math.sin(angle2);

                    float u1 = cutLeft + ((x1 + 1f) / 2f) * (cutRight - cutLeft);
                    float v1 = cutBottom + ((y1 + 1f) / 2f) * (cutTop - cutBottom);

                    float u2 = cutLeft + ((x2 + 1f) / 2f) * (cutRight - cutLeft);
                    float v2 = cutBottom + ((y2 + 1f) / 2f) * (cutTop - cutBottom);

                    buffer.vertex(position, 0, 0, 0).texture(uc, vc);       // Center of circle (origin)
                    buffer.vertex(position, x1, y1, 0).texture(u1, v1);     // First edge point
                    buffer.vertex(position, x2, y2, 0).texture(u2, v2);     // Second edge point
                }
            }
        }


        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        image.init();
        RenderSystem.setShaderTexture(0, image.id);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);

        glBindTexture(GL_TEXTURE_2D, image.id);
        image.buffer.bind();
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image.width, image.height, GL_RGB, GL_UNSIGNED_BYTE, 0);
        image.buffer.unbind();
        image.buffer.writeAndSwap(image.data().duplicate());

        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableCull();
        RenderSystem.setShaderTexture(0, 0);

        matrixStack.pop();

    }
}
