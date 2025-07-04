package me.bbijabnpobatejb.webcam.message;

import me.bbijabnpobatejb.webcam.client.object.PlayerVideo;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static me.bbijabnpobatejb.webcam.WebcamMod.MOD_ID;

public record VideoFramePayload(PlayerVideo video) implements CustomPayload {
    public static final Identifier VIDEO_FRAME_PAYLOAD_ID = Identifier.of(MOD_ID, "video_frame");
    public static final Id<VideoFramePayload> ID = new Id<>(VIDEO_FRAME_PAYLOAD_ID);
    public static final PacketCodec<PacketByteBuf, VideoFramePayload> CODEC = PacketCodec.tuple(PlayerVideoPacketCodec.PACKET_CODEC, VideoFramePayload::video, VideoFramePayload::new);


    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
