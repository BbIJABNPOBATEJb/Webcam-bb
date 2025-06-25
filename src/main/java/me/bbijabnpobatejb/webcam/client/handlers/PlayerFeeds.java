package me.bbijabnpobatejb.webcam.client.handlers;


import lombok.val;
import me.bbijabnpobatejb.webcam.client.object.PlayerVideo;
import me.bbijabnpobatejb.webcam.client.object.RenderImage;

import java.util.HashMap;
import java.util.UUID;

public class PlayerFeeds {
    public final HashMap<UUID, RenderImage> images = new HashMap<>();

    public RenderImage get(UUID uuid) {
        return images.get(uuid);
    }


    public void update(PlayerVideo video) {
        val uuid = video.getUuid();
        RenderImage image = images.getOrDefault(uuid, new RenderImage());
        image.fill(video);
        images.put(uuid, image);


    }
}
