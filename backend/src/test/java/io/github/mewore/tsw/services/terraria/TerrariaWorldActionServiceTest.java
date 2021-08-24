package io.github.mewore.tsw.services.terraria;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mewore.tsw.events.FakeSubscription;
import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.terraria.world.TerrariaWorldEntity;
import io.github.mewore.tsw.services.LocalHostService;
import io.github.mewore.tsw.services.util.async.InterruptableRunnable;
import io.github.mewore.tsw.services.util.async.LifecycleThreadPool;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerrariaWorldActionServiceTest {

    private static final UUID LOCAL_HOST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private static final UUID OTHER_HOST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @InjectMocks
    private TerrariaWorldActionService terrariaWorldActionService;

    @Mock
    private TerrariaWorldApplicationEventService terrariaWorldApplicationEventService;

    @Mock
    private LifecycleThreadPool lifecycleThreadPool;

    @Mock
    private LocalHostService localHostService;

    @Mock
    private TerrariaWorldFileService terrariaWorldFileService;

    @Captor
    private ArgumentCaptor<InterruptableRunnable> waitForWorldDeletionsCaptor;

    private static void assignHostToWorld(final TerrariaWorldEntity world, final UUID uuid) {
        final HostEntity host = mock(HostEntity.class);
        when(world.getHost()).thenReturn(host);
        when(host.getUuid()).thenReturn(uuid);
    }

    @Test
    void testWaitForWorldDeletions() throws InterruptedException {
        final TerrariaWorldEntity worldAtAnotherHost = mock(TerrariaWorldEntity.class);
        final TerrariaWorldEntity world = mock(TerrariaWorldEntity.class);
        when(terrariaWorldApplicationEventService.subscribeToWorldDeletions()).thenReturn(
                new FakeSubscription<>(worldAtAnotherHost, world));

        terrariaWorldActionService.setUp();
        verify(lifecycleThreadPool, only()).run(waitForWorldDeletionsCaptor.capture());

        assignHostToWorld(worldAtAnotherHost, OTHER_HOST_UUID);
        assignHostToWorld(world, LOCAL_HOST_UUID);
        when(localHostService.getHostUuid()).thenReturn(LOCAL_HOST_UUID);

        waitForWorldDeletionsCaptor.getValue().run();
        verify(terrariaWorldFileService, only()).deleteWorldFiles(world);
    }
}