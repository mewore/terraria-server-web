package io.github.mewore.tsw.services.terraria;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.models.EmptyMessage;
import io.github.mewore.tsw.models.terraria.world.TerrariaWorldEntity;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class TerrariaWorldMessageService {

    private static final String PREFIX = "/topic/worlds";

    private final SimpMessagingTemplate messagingTemplate;

    void broadcastWorldDeletion(final TerrariaWorldEntity world) {
        messagingTemplate.send(PREFIX + "/" + world.getId() + "/deletion", new EmptyMessage());
    }
}
