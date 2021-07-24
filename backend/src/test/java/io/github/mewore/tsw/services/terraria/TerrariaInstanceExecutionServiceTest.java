package io.github.mewore.tsw.services.terraria;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import io.github.mewore.tsw.events.FakeSubscription;
import io.github.mewore.tsw.events.Subscription;
import io.github.mewore.tsw.models.file.FileDataEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;
import io.github.mewore.tsw.repositories.file.FileDataRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceEventRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldRepository;
import io.github.mewore.tsw.services.util.FileService;
import io.github.mewore.tsw.services.util.FileTail;
import io.github.mewore.tsw.services.util.process.ProcessFailureException;
import io.github.mewore.tsw.services.util.process.ProcessTimeoutException;
import io.github.mewore.tsw.services.util.process.TmuxService;

import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.INSTANCE_UUID;
import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.makeInstanceWithState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerrariaInstanceExecutionServiceTest {

    @InjectMocks
    private TerrariaInstanceExecutionService terrariaInstanceExecutionService;

    @Mock
    private TerrariaInstanceRepository terrariaInstanceRepository;

    @Mock
    private TerrariaInstancePreparationService terrariaInstancePreparationService;

    @Mock
    private TerrariaInstanceEventRepository terrariaInstanceEventRepository;

    @Mock
    private TerrariaInstanceEventService terrariaInstanceEventService;

    @Mock
    private FileDataRepository fileDataRepository;

    @Mock
    private TerrariaWorldRepository terrariaWorldRepository;

    @Mock
    private TerrariaWorldService terrariaWorldService;

    @Mock
    private TerrariaInstanceOutputService terrariaInstanceOutputService;

    @Mock
    private TerrariaInstanceInputService terrariaInstanceInputService;

    @Mock
    private TmuxService tmuxService;

    @Mock
    private FileService fileService;

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @Captor
    private ArgumentCaptor<TerrariaInstanceState> stateCaptor;

    @Captor
    private ArgumentCaptor<Duration> durationCaptor;

    private static TerrariaWorldEntity makeWorldForInstance(final TerrariaInstanceEntity instance) {
        return makeWorldForInstance(instance, "World");
    }

    private static TerrariaWorldEntity makeWorldForInstance(final TerrariaInstanceEntity instance, final String name) {
        final TerrariaWorldEntity world = TerrariaWorldEntity.builder()
                .name(name)
                .lastModified(Instant.now())
                .data(mock(FileDataEntity.class))
                .mods(Collections.emptySet())
                .host(instance.getHost())
                .build();
        instance.setWorld(world);
        return world;
    }

    @Test
    void testStartInstance() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        instance.setNextOutputBytePosition(10L);
        final Subscription<TerrariaInstanceEntity> subscription = new FakeSubscription<>(null);
        when(terrariaInstanceEventService.subscribe(instance)).thenReturn(subscription);
        when(terrariaInstancePreparationService.saveInstance(instance)).thenAnswer(
                invocation -> invocation.getArgument(0));
        when(terrariaInstanceEventService.waitForInstanceState(instance, subscription, TerrariaInstanceState.WORLD_MENU,
                Duration.ofMinutes(1))).thenReturn(instance);

        final TerrariaInstanceEntity result = terrariaInstanceExecutionService.bootUpInstance(instance);

        assertEquals(0L, instance.getNextOutputBytePosition());
        verify(terrariaInstancePreparationService).ensureInstanceHasNoOutputFile(instance);
        verify(terrariaInstanceOutputService).trackInstance(instance);
        verify(tmuxService).dispatch(INSTANCE_UUID.toString(), instance.getModLoaderServerFile(),
                instance.getOutputFile());
        assertSame(instance, result);
    }

    @Test
    void testStartInstance_incorrectState() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.BROKEN);
        final Exception exception = assertThrows(IllegalArgumentException.class,
                () -> terrariaInstanceExecutionService.bootUpInstance(instance));
        assertEquals("Cannot start an instance with state BROKEN", exception.getMessage());
    }

    @Test
    void testGoToModMenu() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.WORLD_MENU);
        when(terrariaInstanceInputService.sendInputToInstance(instance, "m", TerrariaInstanceState.MOD_MENU,
                Duration.ofSeconds(10))).thenReturn(instance);

        final TerrariaInstanceEntity result = terrariaInstanceExecutionService.goToModMenu(instance);
        assertSame(instance, result);
    }

    @Test
    void testGoToModMenu_incorrectState() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        final Exception exception = assertThrows(IllegalArgumentException.class,
                () -> terrariaInstanceExecutionService.goToModMenu(instance));
        assertEquals("Cannot go to the mod menu while in state IDLE", exception.getMessage());
    }

    @Test
    void testSetInstanceLoadedMods() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.WORLD_MENU);
        instance.setModsToEnable(Set.of("Mod1", "Mod3", "Mod4"));
        instance.acknowledgeMenuOption(1, "Mod1 (enabled)");
        instance.acknowledgeMenuOption(2, "Mod2 (enabled)");
        instance.acknowledgeMenuOption(3, "Mod3 (disabled)");
        instance.acknowledgeMenuOption(4, "Mod4 (disabled)");
        instance.setState(TerrariaInstanceState.MOD_MENU);

        when(terrariaInstancePreparationService.saveInstance(instance)).thenReturn(instance);
        when(terrariaInstanceInputService.sendInputToInstance(same(instance), any(), any(), any())).thenAnswer(
                new FakeModMenu(instance.getOptions()));

        final TerrariaInstanceEntity result = terrariaInstanceExecutionService.setInstanceLoadedMods(instance);
        assertSame(instance, result);

        verify(terrariaInstanceInputService, times(3)).sendInputToInstance(same(instance), stringCaptor.capture(),
                same(TerrariaInstanceState.MOD_MENU), eq(Duration.ofSeconds(30)));
        assertEquals(Arrays.asList("2", "3", "4"),
                stringCaptor.getAllValues().stream().sorted().collect(Collectors.toList()));

        verify(terrariaInstanceInputService).sendInputToInstance(instance, "r", TerrariaInstanceState.WORLD_MENU,
                Duration.ofMinutes(2));
    }

    @Test
    void testSetInstanceLoadedMods_tooManyAttempts()
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.WORLD_MENU);
        instance.setModsToEnable(Set.of("Mod1"));
        instance.acknowledgeMenuOption(1, "Mod1 (disabled)");
        instance.setState(TerrariaInstanceState.MOD_MENU);

        when(terrariaInstancePreparationService.saveInstance(instance)).thenReturn(instance);
        when(terrariaInstanceInputService.sendInputToInstance(same(instance), any(), any(), any())).thenAnswer(
                invocation -> {
                    instance.acknowledgeMenuOption(1, "Mod1 (disabled)");
                    instance.setState(TerrariaInstanceState.MOD_MENU);
                    return instance;
                });

        final Exception exception = assertThrows(RuntimeException.class,
                () -> terrariaInstanceExecutionService.setInstanceLoadedMods(instance));
        assertEquals("Failed to make the following mods enabled after 100 attempts: Mod1", exception.getMessage());
        verify(terrariaInstanceInputService, times(100)).sendInputToInstance(same(instance), any(), any(), any());
        verify(terrariaInstanceInputService, never()).sendInputToInstance(any(), eq("r"), any(), any());
    }

    @Test
    void testSetInstanceLoadedMods_incorrectLoadedModCount()
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.MOD_MENU);
        instance.setLoadedMods(Set.of("Mod1 v1", "Mod2 v1"));

        when(terrariaInstanceInputService.sendInputToInstance(instance, "r", TerrariaInstanceState.WORLD_MENU,
                Duration.ofMinutes(2))).thenReturn(instance);

        final Exception exception = assertThrows(RuntimeException.class,
                () -> terrariaInstanceExecutionService.setInstanceLoadedMods(instance));
        assertEquals("The mods of instance " + INSTANCE_UUID + " (2: Mod1 v1, Mod2 v1) " +
                "are not exactly as many as the requested ones (0: )", exception.getMessage());
    }

    @Test
    void testSetInstanceLoadedMods_unselectableMod() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.WORLD_MENU);
        instance.setModsToEnable(Set.of("Mod1", "Mod3", "Mod4"));
        instance.acknowledgeMenuOption(1, "Mod1 (enabled)");
        instance.acknowledgeMenuOption(2, "Mod2 (enabled)");
        instance.setState(TerrariaInstanceState.MOD_MENU);

        final Exception exception = assertThrows(IllegalArgumentException.class,
                () -> terrariaInstanceExecutionService.setInstanceLoadedMods(instance));
        assertEquals("Cannot enable the following mods because they aren't in the list of known options: Mod3, Mod4",
                exception.getMessage());
    }

    @Test
    void testSetInstanceLoadedMods_incorrectState() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        final Exception exception = assertThrows(IllegalArgumentException.class,
                () -> terrariaInstanceExecutionService.setInstanceLoadedMods(instance));
        assertEquals("Cannot set the mods of an instance with state IDLE", exception.getMessage());
    }

    @Test
    void testRunInstance() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.BOOTING_UP);
        instance.acknowledgeMenuOption(1, "World1");
        instance.acknowledgeMenuOption(2, "World2");
        instance.acknowledgeMenuOption(3, "World3");
        instance.setLoadedMods(Set.of("Mod"));
        instance.setState(TerrariaInstanceState.WORLD_MENU);
        instance.setMaxPlayers(10);
        instance.setPort(8000);
        instance.setAutomaticallyForwardPort(false);
        instance.setPassword("password");

        makeWorldForInstance(instance, "World2");

        when(terrariaInstanceInputService.sendInputToInstance(same(instance), any(), any(), any())).thenReturn(
                instance);
        when(terrariaInstanceInputService.sendInputToInstance(same(instance), any(), any(), any(),
                eq(true))).thenReturn(instance);
        when(terrariaInstancePreparationService.saveInstance(instance)).thenReturn(instance);

        final TerrariaInstanceEntity result = terrariaInstanceExecutionService.runInstance(instance);
        assertSame(instance, result);

        verify(terrariaInstanceInputService, times(4)).sendInputToInstance(same(instance), stringCaptor.capture(),
                stateCaptor.capture(), durationCaptor.capture());
        assertEquals(Arrays.asList("2", "10", "8000", "n"), stringCaptor.getAllValues());
        assertEquals(Arrays.asList(TerrariaInstanceState.MAX_PLAYERS_PROMPT, TerrariaInstanceState.PORT_PROMPT,
                TerrariaInstanceState.AUTOMATICALLY_FORWARD_PORT_PROMPT, TerrariaInstanceState.PASSWORD_PROMPT),
                stateCaptor.getAllValues());
        assertEquals(Arrays.asList(Duration.ofSeconds(10), Duration.ofSeconds(10), Duration.ofSeconds(10),
                Duration.ofSeconds(10)), durationCaptor.getAllValues());

        verify(terrariaInstanceInputService).sendInputToInstance(instance, "password", TerrariaInstanceState.RUNNING,
                Duration.ofSeconds(190), true);

        assertEquals("", instance.getPassword());
    }

    @Test
    void testRunInstance_forwardPort() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.BOOTING_UP);
        instance.acknowledgeMenuOption(1, "World1");
        instance.setState(TerrariaInstanceState.WORLD_MENU);
        instance.setAutomaticallyForwardPort(true);
        instance.setPassword("password");

        makeWorldForInstance(instance, "World1");

        when(terrariaInstanceInputService.sendInputToInstance(same(instance), any(), any(), any())).thenReturn(
                instance);
        when(terrariaInstanceInputService.sendInputToInstance(same(instance), any(), any(), any(),
                anyBoolean())).thenReturn(instance);

        terrariaInstanceExecutionService.runInstance(instance);
        verify(terrariaInstanceInputService).sendInputToInstance(same(instance), eq("y"), any(), any());
    }

    @Test
    void testRunInstance_incorrectState() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        final Exception exception = assertThrows(IllegalArgumentException.class,
                () -> terrariaInstanceExecutionService.runInstance(instance));
        assertEquals("Cannot run an instance with state IDLE", exception.getMessage());
    }

    @Test
    void testRunInstance_noWorld() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.WORLD_MENU);
        final Exception exception = assertThrows(IllegalArgumentException.class,
                () -> terrariaInstanceExecutionService.runInstance(instance));
        assertEquals("Cannot run an instance that does not have an assigned world!", exception.getMessage());
    }

    @Test
    void testRunInstance_noOptions() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.WORLD_MENU);
        instance.setWorld(mock(TerrariaWorldEntity.class));
        final Exception exception = assertThrows(IllegalArgumentException.class,
                () -> terrariaInstanceExecutionService.runInstance(instance));
        assertEquals("Cannot run an instance that does not have any options!", exception.getMessage());
    }

    @Test
    void testRunInstance_noWorldOption() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.BOOTING_UP);
        instance.acknowledgeMenuOption(1, "World1");
        instance.acknowledgeMenuOption(2, "World2");
        instance.setState(TerrariaInstanceState.WORLD_MENU);

        makeWorldForInstance(instance, "World3");

        final Exception exception = assertThrows(IllegalArgumentException.class,
                () -> terrariaInstanceExecutionService.runInstance(instance));
        assertEquals("Cannot run instance " + INSTANCE_UUID + " with world World3 " +
                "because it isn't in the known menu world options:\n1\t\tWorld1\n2\t\tWorld2", exception.getMessage());
    }

    @Test
    void testShutDownInstance() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.RUNNING);
        instance.setLoadedMods(Set.of("Mod"));
        final TerrariaWorldEntity world = makeWorldForInstance(instance);

        final FileTail tail = mock(FileTail.class);
        when(terrariaInstanceOutputService.getInstanceOutputTail(instance)).thenReturn(tail);

        final TerrariaInstanceEntity awaitedInstance = mock(TerrariaInstanceEntity.class);
        when(terrariaInstanceInputService.sendInputToInstance(instance, "exit", TerrariaInstanceState.IDLE,
                Duration.ofSeconds(90))).thenReturn(awaitedInstance);

        final FileDataEntity newWorldData = mock(FileDataEntity.class);
        final Instant newLastModified = Instant.now();
        final TerrariaWorldEntity newWorld = mock(TerrariaWorldEntity.class);
        when(newWorld.getData()).thenReturn(newWorldData);
        when(newWorld.getLastModified()).thenReturn(newLastModified);
        when(terrariaWorldService.readWorld(world)).thenReturn(newWorld);

        final FileDataEntity savedWorldData = mock(FileDataEntity.class);
        when(fileDataRepository.save(newWorldData)).thenReturn(savedWorldData);

        final TerrariaInstanceEntity result = terrariaInstanceExecutionService.shutDownInstance(instance, true);
        assertSame(awaitedInstance, result);

        verify(tail).stopReadingFile();

        assertSame(newLastModified, world.getLastModified());
        assertSame(savedWorldData, world.getData());
        assertEquals(Set.of("Mod"), world.getMods());
        verify(terrariaWorldRepository).save(world);

        verify(terrariaInstanceOutputService).stopTrackingInstance(awaitedInstance);
    }

    @Test
    void testShutDownInstance_failureToReadWorld()
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.RUNNING);
        instance.setLoadedMods(Set.of("Mod"));
        final TerrariaWorldEntity world = makeWorldForInstance(instance);

        final FileTail tail = mock(FileTail.class);
        when(terrariaInstanceOutputService.getInstanceOutputTail(instance)).thenReturn(tail);

        final TerrariaInstanceEntity awaitedInstance = mock(TerrariaInstanceEntity.class);
        when(terrariaInstanceInputService.sendInputToInstance(instance, "exit", TerrariaInstanceState.IDLE,
                Duration.ofSeconds(90))).thenReturn(awaitedInstance);

        when(terrariaWorldService.readWorld(world)).thenReturn(null);

        terrariaInstanceExecutionService.shutDownInstance(instance, true);
        verify(terrariaWorldRepository, never()).save(world);
    }

    @Test
    void testShutDownInstance_noWorld() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.RUNNING);

        when(terrariaInstanceOutputService.getInstanceOutputTail(instance)).thenReturn(mock(FileTail.class));
        when(terrariaInstanceInputService.sendInputToInstance(instance, "exit", TerrariaInstanceState.IDLE,
                Duration.ofSeconds(90))).thenReturn(instance);

        terrariaInstanceExecutionService.shutDownInstance(instance, true);
        verify(terrariaWorldRepository, never()).save(any());
    }

    @Test
    void testShutDownInstance_noSave() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.RUNNING);
        instance.setLoadedMods(Set.of("Mod"));
        final TerrariaWorldEntity world = makeWorldForInstance(instance);

        when(terrariaInstanceOutputService.getInstanceOutputTail(instance)).thenReturn(mock(FileTail.class));

        when(terrariaInstanceInputService.sendInputToInstance(instance, "exit-nosave", TerrariaInstanceState.IDLE,
                Duration.ofSeconds(30))).thenReturn(instance);

        terrariaInstanceExecutionService.shutDownInstance(instance, false);

        assertEquals(Collections.emptySet(), world.getMods());
        verify(terrariaWorldRepository, never()).save(any());
        verify(terrariaInstanceOutputService).stopTrackingInstance(instance);
    }

    @Test
    void testShutDownInstance_notRunning()
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.WORLD_MENU);

        when(terrariaInstanceOutputService.getInstanceOutputTail(instance)).thenReturn(mock(FileTail.class));

        final TerrariaInstanceEntity awaitedInstance = mock(TerrariaInstanceEntity.class);
        when(terrariaInstanceInputService.sendBreakToInstance(instance, TerrariaInstanceState.IDLE,
                Duration.ofSeconds(30))).thenReturn(awaitedInstance);

        final TerrariaInstanceEntity result = terrariaInstanceExecutionService.shutDownInstance(instance, true);
        assertSame(awaitedInstance, result);

        verify(terrariaInstanceInputService, never()).sendInputToInstance(any(), any(), any(), any());
        verify(terrariaWorldRepository, never()).save(any());
        verify(terrariaInstanceOutputService).stopTrackingInstance(awaitedInstance);
    }

    @Test
    void testShutDownInstance_inactiveState() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        final Exception exception = assertThrows(IllegalArgumentException.class,
                () -> terrariaInstanceExecutionService.shutDownInstance(instance, true));
        assertEquals("Cannot shut down an instance with state IDLE", exception.getMessage());
    }

    @Test
    void testTerminateInstance() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.RUNNING);

        when(tmuxService.hasSession(INSTANCE_UUID.toString())).thenReturn(true);
        when(terrariaInstanceOutputService.isTrackingInstance(instance)).thenReturn(true);

        final FileTail tail = mock(FileTail.class);
        when(terrariaInstanceOutputService.getInstanceOutputTail(instance)).thenReturn(tail);

        final Subscription<TerrariaInstanceEntity> subscription = new FakeSubscription<>(null);
        when(terrariaInstanceEventService.subscribe(instance)).thenReturn(subscription);

        final TerrariaInstanceEntity awaitedInstance = mock(TerrariaInstanceEntity.class);
        when(terrariaInstanceEventService.waitForInstanceState(instance, subscription, TerrariaInstanceState.IDLE,
                Duration.ofSeconds(30))).thenReturn(awaitedInstance);

        final TerrariaInstanceEntity result = terrariaInstanceExecutionService.terminateInstance(instance);
        assertSame(awaitedInstance, result);

        verify(tail).stopReadingFile();
        verify(tmuxService).kill(INSTANCE_UUID.toString());
        verify(terrariaInstanceOutputService).stopTrackingInstance(awaitedInstance);
    }

    @Test
    void testTerminateInstance_notTracked()
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.RUNNING);

        when(tmuxService.hasSession(INSTANCE_UUID.toString())).thenReturn(true);
        when(terrariaInstanceOutputService.isTrackingInstance(instance)).thenReturn(false);
        when(terrariaInstanceOutputService.getInstanceOutputTail(instance)).thenReturn(mock(FileTail.class));

        final Subscription<TerrariaInstanceEntity> subscription = new FakeSubscription<>(null);
        when(terrariaInstanceEventService.subscribe(instance)).thenReturn(subscription);

        final TerrariaInstanceEntity awaitedInstance = mock(TerrariaInstanceEntity.class);
        when(terrariaInstanceEventService.waitForInstanceState(instance, subscription, TerrariaInstanceState.IDLE,
                Duration.ofSeconds(30))).thenReturn(awaitedInstance);

        terrariaInstanceExecutionService.terminateInstance(instance);

        verify(terrariaInstanceOutputService).trackInstance(instance);
        verify(tmuxService).kill(INSTANCE_UUID.toString());
        verify(terrariaInstanceOutputService).stopTrackingInstance(awaitedInstance);
    }

    @Test
    void testTerminateInstance_noSession()
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.RUNNING);
        when(tmuxService.hasSession(INSTANCE_UUID.toString())).thenReturn(false);

        terrariaInstanceExecutionService.terminateInstance(instance);
        verify(terrariaInstancePreparationService).saveInstance(instance);
        assertSame(TerrariaInstanceState.IDLE, instance.getState());
        verify(terrariaInstanceOutputService).stopTrackingInstance(instance);
    }

    @Test
    void testTerminateInstance_inactiveState() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        final Exception exception = assertThrows(IllegalArgumentException.class,
                () -> terrariaInstanceExecutionService.terminateInstance(instance));
        assertEquals("Cannot terminate an instance with state IDLE", exception.getMessage());
    }

    @Test
    void testDeleteInstance() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        when(tmuxService.hasSession(INSTANCE_UUID.toString())).thenReturn(false);
        when(fileService.fileExists(same(instance.getLocation()))).thenReturn(true);
        when(fileService.deleteRecursively(instance.getLocation())).thenReturn(true);

        terrariaInstanceExecutionService.deleteInstance(instance);
        verify(terrariaInstanceOutputService).stopTrackingInstance(instance);
        verify(terrariaInstanceEventRepository).deleteByInstance(instance);
        verify(terrariaInstanceRepository).delete(instance);
    }

    @Test
    void testDeleteInstance_withSession()
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        when(tmuxService.hasSession(INSTANCE_UUID.toString())).thenReturn(true);

        terrariaInstanceExecutionService.deleteInstance(instance);
        verify(tmuxService).kill(instance.getUuid().toString());
    }

    @Test
    void testDeleteInstance_noDirectory()
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        when(tmuxService.hasSession(INSTANCE_UUID.toString())).thenReturn(false);
        when(fileService.fileExists(same(instance.getLocation()))).thenReturn(false);

        terrariaInstanceExecutionService.deleteInstance(instance);
        verify(fileService, never()).deleteRecursively(any());
    }

    @Test
    void testDeleteInstance_failureToDelete()
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        when(tmuxService.hasSession(INSTANCE_UUID.toString())).thenReturn(false);
        when(fileService.fileExists(same(instance.getLocation()))).thenReturn(true);
        when(fileService.deleteRecursively(instance.getLocation())).thenReturn(false);

        terrariaInstanceExecutionService.deleteInstance(instance);
    }

    @Test
    void testDeleteInstance_incorrectState() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.RUNNING);
        final Exception exception = assertThrows(IllegalArgumentException.class,
                () -> terrariaInstanceExecutionService.deleteInstance(instance));
        assertEquals("Cannot delete a running instance (with state RUNNING)!", exception.getMessage());
    }

    private static class FakeModMenu implements Answer<TerrariaInstanceEntity> {

        final Map<Integer, String> options;

        private FakeModMenu(final Map<Integer, String> initialOptions) {
            options = new HashMap<>(initialOptions);
        }

        @Override
        public TerrariaInstanceEntity answer(final InvocationOnMock invocation) {
            final TerrariaInstanceEntity instance = invocation.getArgument(0);
            final String input = invocation.getArgument(1);
            if (instance == null || input == null) {
                throw new NullPointerException("This should not happen");
            }
            if (input.equals("r")) {
                instance.setLoadedMods(options.values()
                        .stream()
                        .filter(label -> label.endsWith(" (enabled)"))
                        .map(label -> label.replaceAll(" \\(enabled\\)$", ""))
                        .collect(Collectors.toUnmodifiableSet()));
                return instance;
            }
            final int intKey = Integer.parseInt(input);
            final @Nullable String selectedOptionLabel = options.get(intKey);
            if (selectedOptionLabel == null) {
                return instance;
            }
            options.put(intKey, selectedOptionLabel.replaceAll(" \\(enabled\\)$", " (tmp)")
                    .replaceAll(" \\(disabled\\)$", " (enabled)")
                    .replaceAll(" \\(tmp\\)$", " (disabled)"));
            for (final Map.Entry<Integer, String> option : options.entrySet()) {
                instance.acknowledgeMenuOption(option.getKey(), option.getValue());
            }
            instance.setState(TerrariaInstanceState.MOD_MENU);
            return instance;
        }
    }
}