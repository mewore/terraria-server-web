package io.github.mewore.tsw.services.terraria;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import io.github.mewore.tsw.events.DeletedWorldNotification;
import io.github.mewore.tsw.models.EmptyMessage;
import io.github.mewore.tsw.models.terraria.world.TerrariaWorldEntity;

import static io.github.mewore.tsw.models.terraria.TerrariaWorldFactory.makeWorldBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TerrariaWorldMessageServiceTest {

    @InjectMocks
    private TerrariaWorldMessageService terrariaWorldMessageService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void testBroadcastWorldDeletion() {
        final TerrariaWorldEntity world = makeWorldBuilder().id(8L).build();
        terrariaWorldMessageService.broadcastWorldDeletion(new DeletedWorldNotification(world));
        verify(messagingTemplate, only()).send(eq("/topic/worlds/8/deletion"), any(EmptyMessage.class));
    }
}