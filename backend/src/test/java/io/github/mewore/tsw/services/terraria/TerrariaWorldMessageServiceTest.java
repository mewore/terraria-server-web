package io.github.mewore.tsw.services.terraria;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import io.github.mewore.tsw.models.EmptyMessage;
import io.github.mewore.tsw.models.terraria.world.TerrariaWorldEntity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerrariaWorldMessageServiceTest {

    @InjectMocks
    private TerrariaWorldMessageService terrariaWorldMessageService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void testBroadcastWorldDeletion() {
        final TerrariaWorldEntity world = mock(TerrariaWorldEntity.class);
        when(world.getId()).thenReturn(8L);

        terrariaWorldMessageService.broadcastWorldDeletion(world);
        verify(messagingTemplate, only()).send(eq("/topic/worlds/8/deletion"), any(EmptyMessage.class));
    }
}