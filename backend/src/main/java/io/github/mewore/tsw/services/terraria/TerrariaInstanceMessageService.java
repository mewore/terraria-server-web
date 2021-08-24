package io.github.mewore.tsw.services.terraria;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.models.EmptyMessage;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventMessage;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceMessage;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class TerrariaInstanceMessageService {

    private final SimpMessagingTemplate messagingTemplate;

    void broadcastInstanceCreation(final TerrariaInstanceEntity instance) {
        messagingTemplate.convertAndSend(String.format("/topic/hosts/%d/instances", instance.getHost().getId()),
                instance);
    }

    void broadcastInstanceChange(final TerrariaInstanceEntity instance) {
        messagingTemplate.convertAndSend("/topic/instances/" + instance.getId(), new TerrariaInstanceMessage(instance));
    }

    public void broadcastInstanceDeletion(final TerrariaInstanceEntity instance) {
        messagingTemplate.send("/topic/instances/" + instance.getId() + "/deletion", new EmptyMessage());
    }

    public void broadcastInstanceEventCreation(final TerrariaInstanceEventEntity event) {
        messagingTemplate.convertAndSend("/topic/instances/" + event.getInstance().getId() + "/events",
                new TerrariaInstanceEventMessage(event));
    }
}
