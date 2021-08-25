package io.github.mewore.tsw.services.terraria;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.events.DeletedWorldNotification;
import io.github.mewore.tsw.models.EmptyMessage;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class TerrariaWorldMessageService {

    private static final String PREFIX = "/topic/worlds";

    private final SimpMessagingTemplate messagingTemplate;

    void broadcastWorldDeletion(final DeletedWorldNotification world) {
        messagingTemplate.send(PREFIX + "/" + world.getId() + "/deletion", new EmptyMessage());
    }
}
