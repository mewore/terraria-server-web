package io.github.mewore.tsw.services.terraria;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import io.github.mewore.tsw.models.terraria.TerrariaInstanceAction;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventMessage;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventType;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceMessage;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;

import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.makeInstanceBuilder;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TerrariaMessageServiceTest {

    @InjectMocks
    private TerrariaMessageService terrariaMessageService;

    @Mock
    private SimpMessagingTemplate brokerMessagingTemplate;

    @Captor
    private ArgumentCaptor<TerrariaInstanceMessage> instanceMessageCaptor;

    @Captor
    private ArgumentCaptor<TerrariaInstanceEventMessage> eventMessageCaptor;

    @Test
    void testBroadcastInstance() {
        final TerrariaInstanceEntity instance = makeInstanceBuilder().id(8L)
                .state(TerrariaInstanceState.IDLE)
                .currentAction(TerrariaInstanceAction.BOOT_UP)
                .pendingAction(TerrariaInstanceAction.RUN_SERVER)
                .options(Map.of(1, "option"))
                .build();

        terrariaMessageService.broadcastInstance(instance);
        verify(brokerMessagingTemplate).convertAndSend(eq("/topic/instances/8"), instanceMessageCaptor.capture());
        final TerrariaInstanceMessage sentMessage = instanceMessageCaptor.getValue();
        assertSame(instance.getState(), sentMessage.getState());
        assertSame(instance.getCurrentAction(), sentMessage.getCurrentAction());
        assertSame(instance.getPendingAction(), sentMessage.getPendingAction());
        assertSame(instance.getOptions(), sentMessage.getOptions());
    }

    @Test
    void testBroadcastInstanceEvent() {
        final TerrariaInstanceEventEntity event = TerrariaInstanceEventEntity.builder()
                .type(TerrariaInstanceEventType.INPUT)
                .text("some text")
                .instance(makeInstanceBuilder().id(8L).build())
                .build();

        terrariaMessageService.broadcastInstanceEvent(event);
        verify(brokerMessagingTemplate).convertAndSend(eq("/topic/instances/8/events"), eventMessageCaptor.capture());
        final TerrariaInstanceEventMessage sentMessage = eventMessageCaptor.getValue();
        assertSame(sentMessage.getType(), sentMessage.getType());
        assertSame(sentMessage.getText(), sentMessage.getText());
    }
}