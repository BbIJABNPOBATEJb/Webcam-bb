package me.bbijabnpobatejb.webcam.client.render.gui;

import dev.isxander.yacl3.gui.image.ImageRenderer;
import lombok.val;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;

import static net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity;

public class TestPlayerRenderer implements ImageRenderer {
    @Override
    public int render(DrawContext context, int x, int y, int renderWidth, float tickDelta) {
        val client = MinecraftClient.getInstance();
        val player = client.player;

        int mouseX = (int) (client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth());
        int mouseY = (int) (client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight());
        if (player == null) return 0;

        int size = (int) (renderWidth / 1.5f);
        renderEntityPreview(context, x, y, size, size, 30, 1f, mouseX, mouseY, player);
        return size;
    }

    @Override
    public void close() {

    }


    public static void renderEntityPreview(DrawContext context, int x, int y, int width, int height, int scale, float verticalOffset, float mouseX, float mouseY, LivingEntity entity) {
        int x2 = x + width;
        int y2 = y + height;

        drawEntity(context, x, y, x2, y2, scale, verticalOffset, mouseX, mouseY, entity);
    }
}
