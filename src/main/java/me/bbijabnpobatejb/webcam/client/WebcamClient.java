package me.bbijabnpobatejb.webcam.client;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import me.bbijabnpobatejb.webcam.client.config.ConfigMenu;
import me.bbijabnpobatejb.webcam.client.handlers.CameraManager;
import me.bbijabnpobatejb.webcam.client.handlers.CodecThread;
import me.bbijabnpobatejb.webcam.client.handlers.PlayerFeeds;
import me.bbijabnpobatejb.webcam.client.render.entity.PlayerFaceRenderer;
import me.bbijabnpobatejb.webcam.client.render.gui.TestPlayerRenderer;
import me.bbijabnpobatejb.webcam.client.render.hud.HudRenderer;
import me.bbijabnpobatejb.webcam.compat.flashback.WebcamFlashback;
import me.bbijabnpobatejb.webcam.message.VideoFramePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import static me.bbijabnpobatejb.webcam.WebcamMod.MOD_ID;
import static me.bbijabnpobatejb.webcam.WebcamMod.isFlashbackPresent;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WebcamClient implements ClientModInitializer {
    @Getter
    static WebcamClient instance;
    static KeyBinding openGuiKey;
    static KeyBinding switchCamera;
    final HudRenderer hudRenderer;
    final TestPlayerRenderer testPlayerRenderer;
    final CodecThread codecThread;
    final PlayerFeeds playerFeeds;

    public WebcamClient() {
        instance = this;

        hudRenderer = new HudRenderer();
        testPlayerRenderer = new TestPlayerRenderer();
        codecThread = new CodecThread();
        playerFeeds = new PlayerFeeds();
    }

    @Override
    public void onInitializeClient() {
        ConfigMenu.HANDLER.load();

        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
            if (isFlashbackPresent()) {
                if (WebcamFlashback.isInReplay()) return;
            }
            hudRenderer.renderWebcamOnHud(drawContext);
        });

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.webcam.config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F4,
                "category." + MOD_ID
        ));

        switchCamera = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.webcam.switch",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "category." + MOD_ID
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openGuiKey.wasPressed()) {
                ConfigMenu configMenu = new ConfigMenu();
                Screen screen = configMenu.getModConfigScreenFactory().create(null);
                client.setScreen(screen);
            }
            if (switchCamera.wasPressed()) {
                WebcamClient.getInstance().getCameraManager().switchCamera();
            }


        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (isFlashbackPresent()) {
                if (WebcamFlashback.isInReplay()) return;
            }
            codecThread.start();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            codecThread.stop();
        });

        ClientPlayNetworking.registerGlobalReceiver(VideoFramePayload.ID, ((payload, context) -> {
            playerFeeds.update(payload.video());
            if (isFlashbackPresent()) {
                WebcamFlashback.submitFrame(payload.video());
            }
        }));

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof PlayerEntityRenderer playerEntityRenderer) {
                registrationHelper.register(new PlayerFaceRenderer(playerEntityRenderer));
            }
        });
    }


    public CameraManager getCameraManager() {
        return codecThread.getCameraManager();
    }


}