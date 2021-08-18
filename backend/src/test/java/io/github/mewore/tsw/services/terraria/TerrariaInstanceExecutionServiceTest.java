package io.github.mewore.tsw.services.terraria;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mewore.tsw.events.FakeSubscription;
import io.github.mewore.tsw.events.Subscription;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceAction;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
import io.github.mewore.tsw.models.terraria.world.TerrariaWorldEntity;
import io.github.mewore.tsw.models.terraria.world.WorldDifficultyOption;
import io.github.mewore.tsw.models.terraria.world.WorldSizeOption;
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
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerrariaInstanceExecutionServiceTest {

    @InjectMocks
    private TerrariaInstanceExecutionService terrariaInstanceExecutionService;

    @Mock
    private TerrariaInstanceService terrariaInstanceService;

    @Mock
    private TerrariaInstanceSubscriptionService terrariaInstanceSubscriptionService;

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
                .fileName(name.replace(' ', '_'))
                .displayName(name)
                .lastModified(Instant.now())
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
        when(terrariaInstanceSubscriptionService.subscribe(instance)).thenReturn(subscription);
        when(terrariaInstanceService.saveInstance(instance)).thenAnswer(invocation -> invocation.getArgument(0));
        when(terrariaInstanceSubscriptionService.waitForInstanceState(instance, subscription, Duration.ofMinutes(1),
                TerrariaInstanceState.WORLD_MENU)).thenReturn(instance);

        final TerrariaInstanceEntity result = terrariaInstanceExecutionService.bootUpInstance(instance);

        assertEquals(0L, instance.getNextOutputBytePosition());
        verify(terrariaInstanceService).ensureInstanceHasNoOutputFile(instance);
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
        when(terrariaInstanceInputService.sendInputToInstance(instance, "m", Duration.ofSeconds(10),
                TerrariaInstanceState.MOD_MENU)).thenReturn(instance);

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
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.MOD_MENU);

        when(terrariaInstanceService.getDesiredModOption(instance)).thenReturn(2)
                .thenReturn(3)
                .thenReturn(4)
                .thenReturn(null);

        when(terrariaInstanceService.saveInstance(instance)).thenReturn(instance);
        when(terrariaInstanceInputService.sendInputToInstance(same(instance), any(), any(), any())).thenReturn(
                instance);

        final TerrariaInstanceEntity result = terrariaInstanceExecutionService.setInstanceLoadedMods(instance);
        assertSame(instance, result);

        verify(terrariaInstanceInputService, times(3)).sendInputToInstance(same(instance), stringCaptor.capture(),
                eq(Duration.ofSeconds(30)), same(TerrariaInstanceState.MOD_MENU));
        assertEquals(Arrays.asList("2", "3", "4"),
                stringCaptor.getAllValues().stream().sorted().collect(Collectors.toList()));

        verify(terrariaInstanceInputService).sendInputToInstance(instance, "r", Duration.ofMinutes(2),
                TerrariaInstanceState.WORLD_MENU);
    }

    @Test
    void testSetInstanceLoadedMods_tooManyAttempts()
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.MOD_MENU);
        instance.setModsToEnable(Set.of("Mod1"));

        when(terrariaInstanceService.getDesiredModOption(instance)).thenReturn(1);

        when(terrariaInstanceService.saveInstance(instance)).thenReturn(instance);
        when(terrariaInstanceInputService.sendInputToInstance(same(instance), any(), any(), any())).thenReturn(
                instance);

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

        when(terrariaInstanceService.getDesiredModOption(instance)).thenReturn(null);

        when(terrariaInstanceInputService.sendInputToInstance(instance, "r", Duration.ofMinutes(2),
                TerrariaInstanceState.WORLD_MENU)).thenReturn(instance);

        final Exception exception = assertThrows(RuntimeException.class,
                () -> terrariaInstanceExecutionService.setInstanceLoadedMods(instance));
        assertEquals("The mods of instance " + INSTANCE_UUID + " (2: Mod1 v1, Mod2 v1) " +
                "are not exactly as many as the requested ones (0: )", exception.getMessage());
    }

    @Test
    void testSetInstanceLoadedMods_incorrectState() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        final Exception exception = assertThrows(IllegalArgumentException.class,
                () -> terrariaInstanceExecutionService.setInstanceLoadedMods(instance));
        assertEquals("Cannot set the mods of an instance with state IDLE", exception.getMessage());
    }

    @Test
    void testCreateWorld() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.WORLD_MENU);
        instance.setLoadedMods(Set.of("Mod1"));

        final TerrariaWorldEntity world = makeWorldForInstance(instance, "Some World");
        world.setSize(WorldSizeOption.MEDIUM);
        world.setDifficulty(WorldDifficultyOption.NORMAL);

        when(terrariaInstanceInputService.sendInputToInstance(same(instance), eq("n"), any(),
                same(TerrariaInstanceState.WORLD_SIZE_PROMPT))).thenAnswer(invocation -> {
            instance.acknowledgeMenuOption(1, "Small");
            instance.acknowledgeMenuOption(2, "Medium");
            instance.acknowledgeMenuOption(3, "Large");
            instance.setState(TerrariaInstanceState.WORLD_SIZE_PROMPT);
            return instance;
        });

        when(terrariaInstanceInputService.sendInputToInstance(same(instance), eq("2"), any(),
                same(TerrariaInstanceState.WORLD_DIFFICULTY_PROMPT))).thenAnswer(invocation -> {
            instance.acknowledgeMenuOption(1, "Normal");
            instance.acknowledgeMenuOption(2, "Expert");
            instance.setState(TerrariaInstanceState.WORLD_DIFFICULTY_PROMPT);
            return instance;
        });

        when(terrariaInstanceInputService.sendInputToInstance(same(instance), eq("1"), any(),
                same(TerrariaInstanceState.WORLD_NAME_PROMPT))).thenAnswer(invocation -> {
            instance.setState(TerrariaInstanceState.WORLD_NAME_PROMPT);
            return instance;
        });

        when(terrariaInstanceInputService.sendInputToInstance(same(instance), eq("Some World"), any(),
                same(TerrariaInstanceState.WORLD_MENU))).thenAnswer(invocation -> {
            instance.setState(TerrariaInstanceState.WORLD_MENU);
            return instance;
        });

        when(terrariaInstanceService.saveInstance(instance)).thenReturn(instance);

        final TerrariaInstanceEntity result = terrariaInstanceExecutionService.createWorld(instance);
        assertSame(instance, result);

        verify(terrariaInstanceInputService, times(4)).sendInputToInstance(same(instance), stringCaptor.capture(),
                durationCaptor.capture(), stateCaptor.capture());
        assertEquals(Arrays.asList(Duration.ofSeconds(10), Duration.ofSeconds(10), Duration.ofSeconds(10),
                Duration.ofSeconds(310)), durationCaptor.getAllValues());

        verify(terrariaWorldService).updateWorld(world, Set.of("Mod1"));
    }

    @Test
    void testCreateWorld_incorrectState() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        final Exception exception = assertThrows(IllegalArgumentException.class,
                () -> terrariaInstanceExecutionService.createWorld(instance));
        assertEquals("Cannot create a world with an instance with state IDLE", exception.getMessage());
    }

    @Test
    void testCreateWorld_noWorld() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.WORLD_MENU);
        final Exception exception = assertThrows(IllegalArgumentException.class,
                () -> terrariaInstanceExecutionService.createWorld(instance));
        assertEquals("Cannot create a world with an instance that does not have an assigned world",
                exception.getMessage());
    }

    @Test
    void testCreateWorld_noWorldSize() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.WORLD_MENU);
        makeWorldForInstance(instance, "Some World");

        final Exception exception = assertThrows(IllegalArgumentException.class,
                () -> terrariaInstanceExecutionService.createWorld(instance));
        assertEquals("Cannot create a world with no set size", exception.getMessage());
    }

    @Test
    void testCreateWorld_noWorldDifficulty() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.WORLD_MENU);
        final TerrariaWorldEntity world = makeWorldForInstance(instance, "Some World");
        world.setSize(WorldSizeOption.MEDIUM);

        final Exception exception = assertThrows(IllegalArgumentException.class,
                () -> terrariaInstanceExecutionService.createWorld(instance));
        assertEquals("Cannot create a world with no set difficulty", exception.getMessage());
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
        when(terrariaInstanceInputService.sendInputToInstance(same(instance), any(), any(), eq(true),
                any())).thenReturn(instance);
        when(terrariaInstanceService.saveInstance(instance)).thenReturn(instance);

        final TerrariaInstanceEntity result = terrariaInstanceExecutionService.runInstance(instance);
        assertSame(instance, result);

        verify(terrariaInstanceInputService, times(4)).sendInputToInstance(same(instance), stringCaptor.capture(),
                durationCaptor.capture(), stateCaptor.capture());
        assertEquals(Arrays.asList("2", "10", "8000", "n"), stringCaptor.getAllValues());
        assertEquals(Arrays.asList(TerrariaInstanceState.MAX_PLAYERS_PROMPT, TerrariaInstanceState.PORT_PROMPT,
                        TerrariaInstanceState.AUTOMATICALLY_FORWARD_PORT_PROMPT, TerrariaInstanceState.PASSWORD_PROMPT),
                stateCaptor.getAllValues());
        assertEquals(Arrays.asList(Duration.ofSeconds(10), Duration.ofSeconds(10), Duration.ofSeconds(10),
                Duration.ofSeconds(10)), durationCaptor.getAllValues());

        verify(terrariaInstanceInputService).sendInputToInstance(instance, "password", Duration.ofSeconds(190), true,
                TerrariaInstanceState.RUNNING, TerrariaInstanceState.PORT_CONFLICT);

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
        when(terrariaInstanceInputService.sendInputToInstance(same(instance), any(), any(), anyBoolean(),
                any())).thenReturn(instance);
        when(terrariaInstanceService.saveInstance(instance)).thenReturn(instance);

        terrariaInstanceExecutionService.runInstance(instance);
        verify(terrariaInstanceInputService).sendInputToInstance(same(instance), eq("y"), any(), any());
    }

    @Test
    void testRunInstance_portConflict() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.BOOTING_UP);
        instance.acknowledgeMenuOption(1, "World1");
        instance.setState(TerrariaInstanceState.WORLD_MENU);
        instance.setAutomaticallyForwardPort(true);
        instance.setPassword("password");

        makeWorldForInstance(instance, "World1");

        when(terrariaInstanceInputService.sendInputToInstance(same(instance), any(), any(), any())).thenReturn(
                instance);

        final TerrariaInstanceEntity instanceWithConflict = makeInstanceWithState(TerrariaInstanceState.PORT_CONFLICT);
        when(terrariaInstanceInputService.sendInputToInstance(same(instance), any(), any(), anyBoolean(),
                any())).thenReturn(instanceWithConflict);

        when(terrariaInstanceService.saveInstance(instanceWithConflict)).thenReturn(instanceWithConflict);

        final TerrariaInstanceEntity result = terrariaInstanceExecutionService.runInstance(instance);
        assertSame(instanceWithConflict, result);
        assertSame(TerrariaInstanceAction.SHUT_DOWN, result.getPendingAction());
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
    void testShutDownInstance() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.RUNNING);
        instance.setLoadedMods(Set.of("Mod"));
        final TerrariaWorldEntity world = makeWorldForInstance(instance);

        final FileTail tail = mock(FileTail.class);
        when(terrariaInstanceOutputService.getInstanceOutputTail(instance)).thenReturn(tail);

        final TerrariaInstanceEntity awaitedInstance = mock(TerrariaInstanceEntity.class);
        when(terrariaInstanceInputService.sendInputToInstance(instance, "exit", Duration.ofSeconds(90),
                TerrariaInstanceState.IDLE)).thenReturn(awaitedInstance);
        when(awaitedInstance.getWorld()).thenReturn(world);

        final TerrariaInstanceEntity result = terrariaInstanceExecutionService.shutDownInstance(instance, true);
        assertSame(awaitedInstance, result);

        verify(tail).stopReadingFile();

        verify(terrariaWorldService, only()).updateWorld(world, instance.getLoadedMods());
        verify(terrariaInstanceOutputService).stopTrackingInstance(awaitedInstance);
    }

    @Test
    void testShutDownInstance_noWorld() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.RUNNING);

        when(terrariaInstanceOutputService.getInstanceOutputTail(instance)).thenReturn(mock(FileTail.class));
        when(terrariaInstanceInputService.sendInputToInstance(instance, "exit", Duration.ofSeconds(90),
                TerrariaInstanceState.IDLE)).thenReturn(instance);

        terrariaInstanceExecutionService.shutDownInstance(instance, true);
        verify(terrariaWorldService, never()).updateWorld(any(), any());
    }

    @Test
    void testShutDownInstance_noSave() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.RUNNING);
        instance.setLoadedMods(Set.of("Mod"));
        final TerrariaWorldEntity world = makeWorldForInstance(instance);

        when(terrariaInstanceOutputService.getInstanceOutputTail(instance)).thenReturn(mock(FileTail.class));

        when(terrariaInstanceInputService.sendInputToInstance(instance, "exit-nosave", Duration.ofSeconds(30),
                TerrariaInstanceState.IDLE)).thenReturn(instance);

        terrariaInstanceExecutionService.shutDownInstance(instance, false);

        assertEquals(Collections.emptySet(), world.getMods());
        verify(terrariaWorldService, never()).updateWorld(any(), any());
        verify(terrariaInstanceOutputService).stopTrackingInstance(instance);
    }

    @Test
    void testShutDownInstance_notRunning()
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.WORLD_MENU);

        when(terrariaInstanceOutputService.getInstanceOutputTail(instance)).thenReturn(mock(FileTail.class));

        final TerrariaInstanceEntity awaitedInstance = mock(TerrariaInstanceEntity.class);
        when(terrariaInstanceInputService.sendBreakToInstance(instance, Duration.ofSeconds(30),
                TerrariaInstanceState.IDLE)).thenReturn(awaitedInstance);

        final TerrariaInstanceEntity result = terrariaInstanceExecutionService.shutDownInstance(instance, true);
        assertSame(awaitedInstance, result);

        verify(terrariaInstanceInputService, never()).sendInputToInstance(any(), any(), any(), any());
        verify(terrariaWorldService, never()).updateWorld(any(), any());
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
        when(terrariaInstanceSubscriptionService.subscribe(instance)).thenReturn(subscription);

        final TerrariaInstanceEntity awaitedInstance = mock(TerrariaInstanceEntity.class);
        when(terrariaInstanceSubscriptionService.waitForInstanceState(instance, subscription, Duration.ofSeconds(30),
                TerrariaInstanceState.IDLE)).thenReturn(awaitedInstance);

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
        when(terrariaInstanceSubscriptionService.subscribe(instance)).thenReturn(subscription);

        final TerrariaInstanceEntity awaitedInstance = mock(TerrariaInstanceEntity.class);
        when(terrariaInstanceSubscriptionService.waitForInstanceState(instance, subscription, Duration.ofSeconds(30),
                TerrariaInstanceState.IDLE)).thenReturn(awaitedInstance);

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
        verify(terrariaInstanceService).saveInstance(instance);
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
    void testRecreateInstance() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.BROKEN);

        final TerrariaInstanceEntity result = terrariaInstanceExecutionService.recreateInstance(instance);
        assertSame(instance, result);

        assertEquals(TerrariaInstanceState.DEFINED, instance.getState());
        assertEquals(TerrariaInstanceAction.SET_UP, instance.getPendingAction());
    }

    @Test
    void testRecreateInstance_running() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.BROKEN);
        when(tmuxService.hasSession(eq("aaa24aaa-e6e4-4f8a-982b-004cbb04e505"))).thenReturn(true);

        terrariaInstanceExecutionService.recreateInstance(instance);
        verify(tmuxService).kill(eq("aaa24aaa-e6e4-4f8a-982b-004cbb04e505"));
    }

    @Test
    void testRecreateInstance_tracked() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.BROKEN);
        when(terrariaInstanceOutputService.isTrackingInstance(same(instance))).thenReturn(true);

        terrariaInstanceExecutionService.recreateInstance(instance);
        verify(terrariaInstanceOutputService).stopTrackingInstance(same(instance));
    }

    @Test
    void testRecreateInstance_existingDirectory()
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.BROKEN);
        when(fileService.exists(same(instance.getLocation()))).thenReturn(true);

        terrariaInstanceExecutionService.recreateInstance(instance);
        verify(fileService).deleteRecursively(same(instance.getLocation()));
    }

    @Test
    void testDeleteInstance() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        when(tmuxService.hasSession(INSTANCE_UUID.toString())).thenReturn(false);
        when(fileService.exists(same(instance.getLocation()))).thenReturn(true);
        when(fileService.deleteRecursively(instance.getLocation())).thenReturn(true);

        terrariaInstanceExecutionService.deleteInstance(instance);
        verify(terrariaInstanceOutputService).stopTrackingInstance(instance);
        verify(terrariaInstanceService).deleteInstance(instance);
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
        when(fileService.exists(same(instance.getLocation()))).thenReturn(false);

        terrariaInstanceExecutionService.deleteInstance(instance);
        verify(fileService, never()).deleteRecursively(any());
    }

    @Test
    void testDeleteInstance_failureToDelete()
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        when(tmuxService.hasSession(INSTANCE_UUID.toString())).thenReturn(false);
        when(fileService.exists(same(instance.getLocation()))).thenReturn(true);
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
}