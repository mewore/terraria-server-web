package io.github.mewore.tsw.services.terraria;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventMessage;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceMessage;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class TerrariaMessageService {

    private final SimpMessagingTemplate brokerMessagingTemplate;

    public void broadcastInstance(final TerrariaInstanceEntity instance) {
        brokerMessagingTemplate.convertAndSend("/topic/instances/" + instance.getId(),
                new TerrariaInstanceMessage(instance));
    }

    public void broadcastInstanceEvent(final TerrariaInstanceEventEntity event) {
        brokerMessagingTemplate.convertAndSend("/topic/instances/" + event.getInstance().getId() + "/events",
                new TerrariaInstanceEventMessage(event));
    }
}
