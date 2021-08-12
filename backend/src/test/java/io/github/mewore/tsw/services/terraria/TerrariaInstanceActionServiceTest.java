package io.github.mewore.tsw.services.terraria;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

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
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventType;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;
import io.github.mewore.tsw.services.LocalHostService;
import io.github.mewore.tsw.services.util.AsyncService;
import io.github.mewore.tsw.services.util.process.ProcessFailureException;
import io.github.mewore.tsw.services.util.process.ProcessTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerrariaInstanceActionServiceTest {

    private static final UUID HOST_UUID = UUID.fromString("e0f245dc-e6e4-4f8a-982b-004cbb04e505");

    @InjectMocks
    private TerrariaInstanceActionService terrariaInstanceActionService;

    @Mock
    private LocalHostService localHostService;

    @Mock
    private TerrariaInstanceService terrariaInstanceService;

    @Mock
    private TerrariaInstancePreparationService terrariaInstancePreparationService;

    @Mock
    private TerrariaInstanceExecutionService terrariaInstanceExecutionService;

    @Mock
    private TerrariaInstanceOutputService terrariaInstanceOutputService;

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

    @Captor
    private ArgumentCaptor<TerrariaInstanceEventEntity> instanceEventCaptor;

    private static TerrariaInstanceEntity.TerrariaInstanceEntityBuilder makeInstance() {
        return TerrariaInstanceFactory.makeInstanceBuilder()
                .state(TerrariaInstanceState.DEFINED)
                .error("previous error");
    }

    @Test
    void testSetUp() {
        when(localHostService.getHostUuid()).thenReturn(HOST_UUID);
        final TerrariaInstanceEntity inactiveInstance = mock(TerrariaInstanceEntity.class);
        when(inactiveInstance.getState()).thenReturn(TerrariaInstanceState.IDLE);
        final TerrariaInstanceEntity instanceWithoutOutputFile = mock(TerrariaInstanceEntity.class);
        when(instanceWithoutOutputFile.getState()).thenReturn(TerrariaInstanceState.RUNNING);
        when(instanceWithoutOutputFile.getOutputFile()).thenReturn(mock(File.class));
        final TerrariaInstanceEntity instance = mock(TerrariaInstanceEntity.class);
        when(instance.getState()).thenReturn(TerrariaInstanceState.RUNNING);
        final File outputFile = mock(File.class);
        when(outputFile.exists()).thenReturn(true);
        when(instance.getOutputFile()).thenReturn(outputFile);
        when(terrariaInstanceRepository.findByHostUuid(HOST_UUID)).thenReturn(
                Arrays.asList(inactiveInstance, instanceWithoutOutputFile, instance));

        terrariaInstanceActionService.setUp();
        verify(terrariaInstanceOutputService).trackInstance(instance);
        verify(asyncService, only()).scheduleAtFixedRate(any(), delayCaptor.capture(), periodCaptor.capture());
        assertEquals(Duration.ZERO, delayCaptor.getValue());
        assertEquals(Duration.ofSeconds(10), periodCaptor.getValue());
    }

    @Test
    void testCheckInstances_setUpInstance() throws InvalidInstanceException, IOException {
        final TerrariaInstanceEntity instance = preparePendingAction(TerrariaInstanceState.DEFINED,
                TerrariaInstanceAction.SET_UP);
        when(terrariaInstancePreparationService.setUpInstance(instance)).thenReturn(instance);

        runCheckInstances();

        verify(terrariaInstanceService, times(2)).saveInstance(instanceCaptor.capture());
        assertNull(instance.getPendingAction());
        assertNull(instance.getCurrentAction());
        assertNull(instance.getActionExecutionStartTime());
        assertNull(instance.getError());
    }

    @Test
    void testCheckInstances_bootUp() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = preparePendingAction(TerrariaInstanceState.IDLE,
                TerrariaInstanceAction.BOOT_UP);
        when(terrariaInstanceExecutionService.bootUpInstance(instance)).thenReturn(instance);

        runCheckInstances();
        assertNull(instance.getError());
    }

    @Test
    void testCheckInstances_goToModMenu()
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = preparePendingAction(TerrariaInstanceState.WORLD_MENU,
                TerrariaInstanceAction.GO_TO_MOD_MENU);
        when(terrariaInstanceExecutionService.goToModMenu(instance)).thenReturn(instance);

        runCheckInstances();
        assertNull(instance.getError());
    }

    @Test
    void testCheckInstances_setLoadedMods()
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = preparePendingAction(TerrariaInstanceState.MOD_MENU,
                TerrariaInstanceAction.SET_LOADED_MODS);
        when(terrariaInstanceExecutionService.setInstanceLoadedMods(instance)).thenReturn(instance);

        runCheckInstances();
        assertNull(instance.getError());
    }

    @Test
    void testCheckInstances_runServer() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = preparePendingAction(TerrariaInstanceState.WORLD_MENU,
                TerrariaInstanceAction.RUN_SERVER);
        when(terrariaInstanceExecutionService.runInstance(instance)).thenReturn(instance);

        runCheckInstances();
        assertNull(instance.getError());
    }

    @Test
    void testCheckInstances_shutDown() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = preparePendingAction(TerrariaInstanceState.WORLD_MENU,
                TerrariaInstanceAction.SHUT_DOWN);
        when(terrariaInstanceExecutionService.shutDownInstance(instance, true)).thenReturn(instance);

        runCheckInstances();
        assertNull(instance.getError());
    }

    @Test
    void testCheckInstances_shutDownNoSave()
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = preparePendingAction(TerrariaInstanceState.RUNNING,
                TerrariaInstanceAction.SHUT_DOWN_NO_SAVE);
        when(terrariaInstanceExecutionService.shutDownInstance(instance, false)).thenReturn(instance);

        runCheckInstances();
        assertNull(instance.getError());
    }

    @Test
    void testCheckInstances_terminate() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = preparePendingAction(TerrariaInstanceState.WORLD_MENU,
                TerrariaInstanceAction.TERMINATE);
        when(terrariaInstanceExecutionService.terminateInstance(instance)).thenReturn(instance);

        runCheckInstances();
        assertNull(instance.getError());
    }

    @Test
    void testCheckInstances_recreate() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = preparePendingAction(TerrariaInstanceState.BROKEN,
                TerrariaInstanceAction.RECREATE);
        when(terrariaInstanceExecutionService.recreateInstance(instance)).thenReturn(instance);

        runCheckInstances();
        assertNull(instance.getError());
    }

    @Test
    void testCheckInstances_delete() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = preparePendingAction(TerrariaInstanceState.IDLE,
                TerrariaInstanceAction.DELETE);

        runCheckInstances();
        assertNull(instance.getError());
        verify(terrariaInstanceExecutionService, only()).deleteInstance(instance);
    }

    @Test
    void testCheckInstances_inapplicableAction() {
        final TerrariaInstanceEntity instance = preparePendingAction(TerrariaInstanceState.RUNNING,
                TerrariaInstanceAction.DELETE);

        runCheckInstances();
        assertNull(instance.getError());
        assertNull(instance.getPendingAction());
        verify(terrariaInstanceService).saveInstance(instance);
    }

    private TerrariaInstanceEntity preparePendingAction(final TerrariaInstanceState state,
            final TerrariaInstanceAction action) {
        when(localHostService.getHostUuid()).thenReturn(HOST_UUID);
        final TerrariaInstanceEntity instance = makeInstance().state(state).pendingAction(action).build();
        when(terrariaInstanceRepository.findTopByHostUuidAndPendingActionNotNull(HOST_UUID)).thenReturn(
                Optional.of(instance));
        when(terrariaInstanceService.saveInstance(instance)).thenReturn(instance);
        return instance;
    }

    @Test
    void testCheckInstances_nullAction() {
        final HostEntity host = mock(HostEntity.class);
        when(localHostService.getHostUuid()).thenReturn(HOST_UUID);
        final TerrariaInstanceEntity instance = makeInstance().host(host).build();
        when(terrariaInstanceRepository.findTopByHostUuidAndPendingActionNotNull(HOST_UUID)).thenReturn(
                Optional.of(instance));

        runCheckInstances();

        verify(terrariaInstanceRepository, never()).save(any());
    }

    @Test
    void testCheckInstances_InvalidInstanceException() throws InvalidInstanceException, IOException {
        when(localHostService.getHostUuid()).thenReturn(HOST_UUID);

        final TerrariaInstanceEntity instance = makeInstance().pendingAction(TerrariaInstanceAction.SET_UP)
                .host(mock(HostEntity.class))
                .build();
        when(terrariaInstanceRepository.findTopByHostUuidAndPendingActionNotNull(HOST_UUID)).thenReturn(
                Optional.of(instance));
        when(terrariaInstanceService.saveInstance(instance)).thenReturn(instance);
        when(terrariaInstanceService.saveInstanceAndEvent(same(instance), any())).thenReturn(instance);
        when(terrariaInstancePreparationService.setUpInstance(any())).thenThrow(new InvalidInstanceException("error"));

        runCheckInstances();

        verify(terrariaInstanceService).saveInstance(instanceCaptor.capture());
        final TerrariaInstanceEntity firstSavedInstance = instanceCaptor.getValue();
        assertNull(firstSavedInstance.getPendingAction());
        assertNull(firstSavedInstance.getCurrentAction());
        assertNull(firstSavedInstance.getActionExecutionStartTime());
        assertSame(TerrariaInstanceState.INVALID, firstSavedInstance.getState());
        assertEquals("error", firstSavedInstance.getError());
        verify(terrariaInstanceService).saveInstanceAndEvent(any(), instanceEventCaptor.capture());

        final TerrariaInstanceEventEntity event = instanceEventCaptor.getValue();
        assertSame(TerrariaInstanceEventType.INVALID_INSTANCE, event.getType());
        assertSame(firstSavedInstance.getError(), event.getText());
        assertSame(instance, event.getInstance());
    }

    @Test
    void testCheckInstances_IllegalArgumentException() throws InvalidInstanceException, IOException {
        when(localHostService.getHostUuid()).thenReturn(HOST_UUID);

        final TerrariaInstanceEntity instance = makeInstance().pendingAction(TerrariaInstanceAction.SET_UP)
                .host(mock(HostEntity.class))
                .build();
        when(terrariaInstanceRepository.findTopByHostUuidAndPendingActionNotNull(HOST_UUID)).thenReturn(
                Optional.of(instance));
        when(terrariaInstanceService.saveInstance(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(terrariaInstanceService.saveInstanceAndEvent(any(), any())).thenAnswer(
                invocation -> invocation.getArgument(0));
        when(terrariaInstancePreparationService.setUpInstance(any())).thenThrow(new IllegalArgumentException("error"));

        runCheckInstances();

        verify(terrariaInstanceService).saveInstance(instanceCaptor.capture());
        final TerrariaInstanceEntity finalSavedInstance = instanceCaptor.getValue();
        assertSame(TerrariaInstanceState.DEFINED, finalSavedInstance.getState());
        assertEquals("error", finalSavedInstance.getError());
        verify(terrariaInstanceService).saveInstanceAndEvent(any(), instanceEventCaptor.capture());

        final TerrariaInstanceEventEntity event = instanceEventCaptor.getValue();
        assertSame(TerrariaInstanceEventType.ERROR, event.getType());
        assertSame(finalSavedInstance.getError(), event.getText());
        assertSame(instance, event.getInstance());
    }

    @Test
    void testCheckInstances_IOException() throws InvalidInstanceException, IOException {
        when(localHostService.getHostUuid()).thenReturn(HOST_UUID);

        final TerrariaInstanceEntity instance = makeInstance().pendingAction(TerrariaInstanceAction.SET_UP)
                .host(mock(HostEntity.class))
                .build();
        when(terrariaInstanceRepository.findTopByHostUuidAndPendingActionNotNull(HOST_UUID)).thenReturn(
                Optional.of(instance));
        when(terrariaInstanceService.saveInstance(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(terrariaInstanceService.saveInstanceAndEvent(any(), any())).thenAnswer(
                invocation -> invocation.getArgument(0));
        when(terrariaInstancePreparationService.setUpInstance(any())).thenThrow(new IOException("error"));

        runCheckInstances();

        verify(terrariaInstanceService).saveInstance(instanceCaptor.capture());
        final TerrariaInstanceEntity finalSavedInstance = instanceCaptor.getValue();
        assertSame(TerrariaInstanceState.BROKEN, finalSavedInstance.getState());
        assertEquals("java.io.IOException: error", finalSavedInstance.getError());

        verify(terrariaInstanceService).saveInstanceAndEvent(any(), instanceEventCaptor.capture());
        assertSame(TerrariaInstanceEventType.ERROR, instanceEventCaptor.getValue().getType());
    }

    @Test
    void testCheckInstances_RuntimeException() throws InvalidInstanceException, IOException {
        when(localHostService.getHostUuid()).thenReturn(HOST_UUID);

        final TerrariaInstanceEntity instance = makeInstance().pendingAction(TerrariaInstanceAction.SET_UP)
                .host(mock(HostEntity.class))
                .build();
        when(terrariaInstanceRepository.findTopByHostUuidAndPendingActionNotNull(HOST_UUID)).thenReturn(
                Optional.of(instance));
        when(terrariaInstanceService.saveInstance(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(terrariaInstanceService.saveInstanceAndEvent(any(), any())).thenAnswer(
                invocation -> invocation.getArgument(0));
        when(terrariaInstancePreparationService.setUpInstance(any())).thenThrow(new RuntimeException("error"));

        runCheckInstances();

        verify(terrariaInstanceService).saveInstance(instanceCaptor.capture());
        assertSame(TerrariaInstanceState.BROKEN, instanceCaptor.getValue().getState());
        assertEquals("error", instanceCaptor.getValue().getError());

        verify(terrariaInstanceService).saveInstanceAndEvent(any(), instanceEventCaptor.capture());
        assertSame(TerrariaInstanceEventType.ERROR, instanceEventCaptor.getValue().getType());
    }

    @Test
    void testCheckInstances_RuntimeException_noMessage() throws InvalidInstanceException, IOException {
        when(localHostService.getHostUuid()).thenReturn(HOST_UUID);

        final TerrariaInstanceEntity instance = makeInstance().pendingAction(TerrariaInstanceAction.SET_UP)
                .host(mock(HostEntity.class))
                .build();
        when(terrariaInstanceRepository.findTopByHostUuidAndPendingActionNotNull(HOST_UUID)).thenReturn(
                Optional.of(instance));
        when(terrariaInstanceService.saveInstance(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(terrariaInstanceService.saveInstanceAndEvent(any(), any())).thenAnswer(
                invocation -> invocation.getArgument(0));
        when(terrariaInstancePreparationService.setUpInstance(any())).thenThrow(new RuntimeException());

        runCheckInstances();

        verify(terrariaInstanceService).saveInstance(instanceCaptor.capture());
        assertSame(TerrariaInstanceState.BROKEN, instanceCaptor.getValue().getState());
        assertEquals("RuntimeException", instanceCaptor.getValue().getError());

        verify(terrariaInstanceService).saveInstanceAndEvent(any(), instanceEventCaptor.capture());
        assertSame(TerrariaInstanceEventType.ERROR, instanceEventCaptor.getValue().getType());
        assertSame(instanceCaptor.getValue().getError(), instanceEventCaptor.getValue().getText());
    }

    @Test
    void testCheckInstances_InterruptedException()
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        when(localHostService.getHostUuid()).thenReturn(HOST_UUID);

        final TerrariaInstanceEntity instance = makeInstance().pendingAction(TerrariaInstanceAction.DELETE)
                .host(mock(HostEntity.class))
                .build();
        when(terrariaInstanceRepository.findTopByHostUuidAndPendingActionNotNull(HOST_UUID)).thenReturn(
                Optional.of(instance));
        when(terrariaInstanceService.saveInstance(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(terrariaInstanceService.saveInstanceAndEvent(any(), any())).thenAnswer(
                invocation -> invocation.getArgument(0));
        doThrow(new InterruptedException("error")).when(terrariaInstanceExecutionService).deleteInstance(any());

        runCheckInstances();

        verify(terrariaInstanceService).saveInstance(instanceCaptor.capture());
        assertSame(TerrariaInstanceState.BROKEN, instanceCaptor.getValue().getState());
        assertEquals("error", instanceCaptor.getValue().getError());

        verify(terrariaInstanceService).saveInstanceAndEvent(any(), instanceEventCaptor.capture());
        assertSame(TerrariaInstanceEventType.TSW_INTERRUPTED, instanceEventCaptor.getValue().getType());
    }

    private void runCheckInstances() {
        terrariaInstanceActionService.setUp();
        verify(asyncService, only()).scheduleAtFixedRate(checkInstancesCaptor.capture(), any(), any());
        checkInstancesCaptor.getValue().run();
    }
}