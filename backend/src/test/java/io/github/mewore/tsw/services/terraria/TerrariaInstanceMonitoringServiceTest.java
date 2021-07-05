package io.github.mewore.tsw.services.terraria;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mewore.tsw.exceptions.InvalidInstanceException;
import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceAction;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;
import io.github.mewore.tsw.services.LocalHostService;
import io.github.mewore.tsw.services.util.AsyncService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerrariaInstanceMonitoringServiceTest {

    @InjectMocks
    private TerrariaInstanceMonitoringService terrariaInstanceMonitoringService;

    @Mock
    private LocalHostService localHostService;

    @Mock
    private TerrariaInstanceService terrariaInstanceService;

    @Mock
    private TerrariaInstanceRepository terrariaInstanceRepository;

    @Mock
    private AsyncService asyncService;

    @Captor
    private ArgumentCaptor<Duration> delayCaptor;

    @Captor
    private ArgumentCaptor<Duration> periodCaptor;

    @Captor
    private ArgumentCaptor<Runnable> checkInstancesCaptor;

    @Captor
    private ArgumentCaptor<TerrariaInstanceEntity> instanceCaptor;

    private static TerrariaInstanceEntity.TerrariaInstanceEntityBuilder makeInstance() {
        return TerrariaInstanceEntity.builder()
                .location(Path.of("instance-dir"))
                .name("Instance Name")
                .terrariaServerUrl("http://terraria.org/server/terraria-server-1003.zip")
                .modLoaderReleaseId(8L)
                .state(TerrariaInstanceState.DEFINED)
                .host(mock(HostEntity.class));
    }

    @Test
    void testSetUp() {
        terrariaInstanceMonitoringService.setUp();
        verify(asyncService, only()).scheduleAtFixedRate(any(), delayCaptor.capture(), periodCaptor.capture());
        assertEquals(Duration.ZERO, delayCaptor.getValue());
        assertEquals(Duration.ofSeconds(10), periodCaptor.getValue());
    }

    @Test
    void testCheckInstances_setUpInstance() throws InvalidInstanceException, IOException {
        final HostEntity host = mock(HostEntity.class);
        when(localHostService.getOrCreateHost()).thenReturn(host);

        final TerrariaInstanceEntity instance =
                makeInstance().pendingAction(TerrariaInstanceAction.SET_UP).host(host).build();
        when(terrariaInstanceRepository.findOneByHostAndPendingActionNotNull(host)).thenReturn(Optional.of(instance));
        when(terrariaInstanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(terrariaInstanceService.setUpTerrariaInstance(any())).thenAnswer(invocation -> invocation.getArgument(0));

        runCheckInstances();

        verify(terrariaInstanceRepository).save(instanceCaptor.capture());
        assertNotNull(instanceCaptor.getValue().getActionExecutionStartTime());
    }

    @Test
    void testCheckInstances_nullAction() {
        final HostEntity host = mock(HostEntity.class);
        when(localHostService.getOrCreateHost()).thenReturn(host);
        final TerrariaInstanceEntity instance = makeInstance().host(host).build();
        when(terrariaInstanceRepository.findOneByHostAndPendingActionNotNull(host)).thenReturn(Optional.of(instance));

        runCheckInstances();

        verify(terrariaInstanceRepository, never()).save(any());
    }

    @Test
    void testCheckInstances_InvalidInstanceException() throws InvalidInstanceException, IOException {
        final HostEntity host = mock(HostEntity.class);
        when(localHostService.getOrCreateHost()).thenReturn(host);

        final TerrariaInstanceEntity instance =
                makeInstance().pendingAction(TerrariaInstanceAction.SET_UP).host(host).build();
        when(terrariaInstanceRepository.findOneByHostAndPendingActionNotNull(host)).thenReturn(Optional.of(instance));
        when(terrariaInstanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(terrariaInstanceService.setUpTerrariaInstance(any())).thenThrow(new InvalidInstanceException("error"));

        runCheckInstances();

        verify(terrariaInstanceRepository, times(2)).save(instanceCaptor.capture());
        final TerrariaInstanceEntity finalSavedInstance = instanceCaptor.getValue();
        assertNull(finalSavedInstance.getPendingAction());
        assertNull(finalSavedInstance.getActionExecutionStartTime());
        assertSame(TerrariaInstanceState.INVALID, finalSavedInstance.getState());
        assertEquals("error", finalSavedInstance.getError());
    }

    @Test
    void testCheckInstances_IOException() throws InvalidInstanceException, IOException {
        final HostEntity host = mock(HostEntity.class);
        when(localHostService.getOrCreateHost()).thenReturn(host);

        final TerrariaInstanceEntity instance =
                makeInstance().pendingAction(TerrariaInstanceAction.SET_UP).host(host).build();
        when(terrariaInstanceRepository.findOneByHostAndPendingActionNotNull(host)).thenReturn(Optional.of(instance));
        when(terrariaInstanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(terrariaInstanceService.setUpTerrariaInstance(any())).thenThrow(new IOException("error"));

        runCheckInstances();

        verify(terrariaInstanceRepository, times(2)).save(instanceCaptor.capture());
        final TerrariaInstanceEntity finalSavedInstance = instanceCaptor.getValue();
        assertNull(finalSavedInstance.getPendingAction());
        assertNull(finalSavedInstance.getActionExecutionStartTime());
        assertSame(TerrariaInstanceState.BROKEN, finalSavedInstance.getState());
        assertEquals("error", finalSavedInstance.getError());
    }

    @Test
    void testCheckInstances_RuntimeException() throws InvalidInstanceException, IOException {
        final HostEntity host = mock(HostEntity.class);
        when(localHostService.getOrCreateHost()).thenReturn(host);

        final TerrariaInstanceEntity instance =
                makeInstance().pendingAction(TerrariaInstanceAction.SET_UP).host(host).build();
        when(terrariaInstanceRepository.findOneByHostAndPendingActionNotNull(host)).thenReturn(Optional.of(instance));
        when(terrariaInstanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(terrariaInstanceService.setUpTerrariaInstance(any())).thenThrow(new RuntimeException("error"));

        runCheckInstances();

        verify(terrariaInstanceRepository, times(2)).save(instanceCaptor.capture());
        assertSame(TerrariaInstanceState.BROKEN, instanceCaptor.getValue().getState());
    }

    private void runCheckInstances() {
        terrariaInstanceMonitoringService.setUp();
        verify(asyncService, only()).scheduleAtFixedRate(checkInstancesCaptor.capture(), any(), any());
        checkInstancesCaptor.getValue().run();
    }
}