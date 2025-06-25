package me.bbijabnpobatejb.webcam.message;

import me.bbijabnpobatejb.webcam.WebcamMod;
import me.bbijabnpobatejb.webcam.client.object.PlayerVideo;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.UUID;

public class PlayerVideoPacketCodec implements PacketCodec<PacketByteBuf, PlayerVideo> {
    public static final PlayerVideoPacketCodec PACKET_CODEC = new PlayerVideoPacketCodec();

    @Override
    public PlayerVideo decode(PacketByteBuf buf) {
        try {
            UUID playerUUID = buf.readUuid();
            int width = buf.readInt();
            int height = buf.readInt();
            int frameBytes = buf.readInt();
            byte[] frame = new byte[frameBytes];
            for (int i = 0; i < frameBytes; i++) {
                frame[i] = buf.readByte();
            }

            PlayerVideo playerVideo = new PlayerVideo(width, height, playerUUID);
            playerVideo.setFrame(frame);
            return playerVideo;
        } catch (Exception e) {
            WebcamMod.LOGGER.error("ERROR DECODING", e);
            throw e;
        }
    }

    @Override
    public void encode(PacketByteBuf buf, PlayerVideo value) {
        buf.writeUuid(value.getUuid());

        buf.writeInt(value.getWidth());
        buf.writeInt(value.getHeight());
        buf.writeInt(value.getFrame().length);
        buf.writeBytes(value.getFrame());
    }


}
