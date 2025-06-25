package me.bbijabnpobatejb.webcam.compat.flashback;

import com.moulberry.flashback.action.Action;
import com.moulberry.flashback.playback.ReplayPlayer;
import com.moulberry.flashback.playback.ReplayServer;
import me.bbijabnpobatejb.webcam.client.object.PlayerVideo;
import me.bbijabnpobatejb.webcam.message.PlayerVideoPacketCodec;
import me.bbijabnpobatejb.webcam.message.VideoFramePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.Identifier;

import static me.bbijabnpobatejb.webcam.WebcamMod.MOD_ID;

public class ActionWebcamFrame implements Action {
    private static final Identifier NAME = Identifier.of(MOD_ID, "action/webcam_frame");
    public static final ActionWebcamFrame INSTANCE = new ActionWebcamFrame();

    private ActionWebcamFrame() {
    }

    @Override
    public Identifier name() {
        return NAME;
    }

    @Override
    public void handle(ReplayServer replayServer, RegistryByteBuf buf) {
        PlayerVideo playerVideo = PlayerVideoPacketCodec.PACKET_CODEC.decode(buf);

        if (replayServer.isProcessingSnapshot) return;

        boolean sendVideo = !replayServer.fastForwarding && !replayServer.replayPaused;

        if (sendVideo) {
            for (ReplayPlayer viewer : replayServer.getReplayViewers()) {
                ServerPlayNetworking.send(viewer, new VideoFramePayload(playerVideo));
            }
        }
    }


}