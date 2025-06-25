package me.bbijabnpobatejb.webcam;

import lombok.val;
import me.bbijabnpobatejb.webcam.compat.flashback.WebcamFlashback;
import me.bbijabnpobatejb.webcam.message.VideoFramePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebcamMod implements ModInitializer {
    public static final String MOD_ID = "webcam-bb";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Webcam-bb mod initialized");


        PayloadTypeRegistry.playC2S().register(VideoFramePayload.ID, VideoFramePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(VideoFramePayload.ID, VideoFramePayload.CODEC);

        if (isFlashbackPresent()) {
            WebcamFlashback.register();
        }
        ServerPlayNetworking.registerGlobalReceiver(VideoFramePayload.ID, (payload, context) -> {
            if (isFlashbackPresent()) {
                WebcamFlashback.sendDefault(payload, context);
            } else {
                sendDefault(payload, context);
            }
        });

    }

    public static void sendDefault(VideoFramePayload payload, ServerPlayNetworking.Context context) {
        val sender = context.player();
        payload.video().setUuid(sender.getUuid());
        for (ServerPlayerEntity player : PlayerLookup.around(sender.getServerWorld(), sender.getPos(), 100)) {
            if (player.getUuid() == sender.getUuid()) {
                continue;
            }

            ServerPlayNetworking.send(player, payload);
        }
    }

    public static boolean isFlashbackPresent() {
        return FabricLoader.getInstance().isModLoaded("flashback");
    }

    public static void showToastText(String title, String text) {
        showToastText(Text.translatable("text." + MOD_ID + "." + title), Text.translatable("text." + MOD_ID + "." + text));
    }

    public static void showToastText(Text title, Text text) {
        ToastManager toastManager = MinecraftClient.getInstance().getToastManager();
        SystemToast.show(toastManager, new SystemToast.Type(), title, text);
    }
}