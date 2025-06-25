package me.bbijabnpobatejb.webcam.client.render.entity;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.experimental.UtilityClass;
import me.bbijabnpobatejb.webcam.client.WebcamClient;
import me.bbijabnpobatejb.webcam.client.object.RenderImage;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityAttachmentType;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import static me.bbijabnpobatejb.webcam.client.config.ConfigMenu.*;
import static org.lwjgl.opengl.GL33.*;

@UtilityClass
public class PlayerFixedRenderer {


    public void renderOnPlayer(MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity entity, PlayerEntityModel<AbstractClientPlayerEntity> context, @Nullable EntityRenderDispatcher dispatcher, float tickDelta) {
        if (!playerRenderEnabled) return;

        if (playerRotationMode != RotationMode.FIXED) return;

        RenderImage image = WebcamClient.getInstance().getPlayerFeeds().get(entity.getUuid());
        if (image == null || image.data() == null) {
            return;
        }

        matrixStack.push();


        Vec3d vec3d = entity.getAttachments().getPointNullable(EntityAttachmentType.NAME_TAG, 0, entity.getYaw(tickDelta));
        if (vec3d == null) return;
        matrixStack.translate(vec3d.x, vec3d.y + 0.5, vec3d.z);

        if (dispatcher != null) {
            matrixStack.multiply(dispatcher.getRotation());
        }
        float y = playerOffsetY + .7f;
        matrixStack.translate(playerOffsetX, y, playerOffsetZ);
        matrixStack.scale(playerRenderScale, playerRenderScale, 1f);

        float widthCamera = image.width;
        float heightCamera = image.height;


        MatrixStack.Entry entry = matrixStack.peek();
        Matrix4f position = entry.getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE);

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
        switch (playerShapeMode) {
            case SQUARE -> {

                buffer.vertex(position, -0.5f, 0.5f, 0).texture(cutRight, cutBottom);  // top-left
                buffer.vertex(position, -0.5f, -0.5f, 0).texture(cutRight, cutTop);     // bottom-left
                buffer.vertex(position, 0.5f, -0.5f, 0).texture(cutLeft, cutTop);     // bottom-right

                buffer.vertex(position, -0.5f, 0.5f, 0).texture(cutRight, cutBottom);  // top-left
                buffer.vertex(position, 0.5f, -0.5f, 0).texture(cutLeft, cutTop);     // bottom-right
                buffer.vertex(position, 0.5f, 0.5f, 0).texture(cutLeft, cutBottom);  // top-right
            }

            case CIRCLE -> {
                int segments = 32;


                float uc = (cutLeft + cutRight) / 2f;
                float vc = (cutBottom + cutTop) / 2f;

                for (int i = 0; i < segments; i++) {
                    float angle1 = (float) (i * 2 * Math.PI / segments);
                    float angle2 = (float) ((i + 1) * 2 * Math.PI / segments);

                    float x1 = (float) Math.cos(angle1) * 0.5f;
                    float y1 = (float) Math.sin(angle1) * 0.5f;

                    float x2 = (float) Math.cos(angle2) * 0.5f;
                    float y2 = (float) Math.sin(angle2) * 0.5f;

                    float u1 = cutRight - (x1 + 0.5f) * (cutRight - cutLeft);
                    float v1 = cutTop - (y1 + 0.5f) * (cutTop - cutBottom);

                    float u2 = cutRight - (x2 + 0.5f) * (cutRight - cutLeft);
                    float v2 = cutTop - (y2 + 0.5f) * (cutTop - cutBottom);

                    buffer.vertex(position, 0, 0, 0).texture(uc, vc);
                    buffer.vertex(position, x1, y1, 0).texture(u1, v1);
                    buffer.vertex(position, x2, y2, 0).texture(u2, v2);
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
