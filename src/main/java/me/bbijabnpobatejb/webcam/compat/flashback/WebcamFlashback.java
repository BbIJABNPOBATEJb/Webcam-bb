package me.bbijabnpobatejb.webcam.compat.flashback;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.action.ActionRegistry;
import lombok.experimental.UtilityClass;
import me.bbijabnpobatejb.webcam.WebcamMod;
import me.bbijabnpobatejb.webcam.client.WebcamClient;
import me.bbijabnpobatejb.webcam.client.object.PlayerVideo;
import me.bbijabnpobatejb.webcam.message.PlayerVideoPacketCodec;
import me.bbijabnpobatejb.webcam.message.VideoFramePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;

@UtilityClass
public class WebcamFlashback {
    public void register(){
        ActionRegistry.register(ActionWebcamFrame.INSTANCE);
    }
    public void sendDefault(VideoFramePayload payload, ServerPlayNetworking.Context context) {
        if (isInReplay()) {
            WebcamClient.getInstance().getPlayerFeeds().update(payload.video());
        } else {
            WebcamMod.sendDefault(payload, context);
        }
    }


    public boolean isInReplay() {
        return Flashback.isInReplay();
    }

    private boolean shouldWritePacket() {
        return Flashback.RECORDER != null && Flashback.RECORDER.readyToWrite();
    }

    public void submitFrame(PlayerVideo video) {
        if (!shouldWritePacket()) return;

        MinecraftClient.getInstance().submit(() -> {
            if (shouldWritePacket()) {
                Flashback.RECORDER.submitCustomTask(writer -> {
                    writer.startAction(ActionWebcamFrame.INSTANCE);
                    PlayerVideoPacketCodec.PACKET_CODEC.encode(writer.friendlyByteBuf(), video);
                    writer.finishAction(ActionWebcamFrame.INSTANCE);
                });
            }
        });
    }
}
