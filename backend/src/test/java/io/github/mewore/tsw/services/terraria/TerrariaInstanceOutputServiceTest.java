package io.github.mewore.tsw.services.terraria;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceEventRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;
import io.github.mewore.tsw.services.util.FileService;
import io.github.mewore.tsw.services.util.FileTail;
import io.github.mewore.tsw.services.util.FileTailEventConsumer;
import io.github.mewore.tsw.services.util.process.ProcessFailureException;
import io.github.mewore.tsw.services.util.process.ProcessTimeoutException;
import io.github.mewore.tsw.services.util.process.TmuxService;

import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.INSTANCE_ID;
import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.INSTANCE_UUID;
import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.makeInstance;
import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.makeInstanceWithState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerrariaInstanceOutputServiceTest {

    @InjectMocks
    private TerrariaInstanceOutputService terrariaInstanceOutputService;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private FileService fileService;

    @Mock
    private TerrariaInstanceRepository terrariaInstanceRepository;

    @Mock
    private TerrariaInstanceEventRepository terrariaInstanceEventRepository;

    @Mock
    private TerrariaInstanceService terrariaInstanceService;

    @Mock
    private TmuxService tmuxService;

    @Captor
    private ArgumentCaptor<FileTailEventConsumer> tailEventConsumerCaptor;

    @Captor
    private ArgumentCaptor<Iterable<TerrariaInstanceEventEntity>> instanceEventCaptor;

    @Test
    void testTrackInstance() {
        final TerrariaInstanceEntity instance = mock(TerrariaInstanceEntity.class);
        when(instance.getId()).thenReturn(INSTANCE_ID);
        when(instance.getNextOutputBytePosition()).thenReturn(10L);
        final File outputFile = mock(File.class);
        when(instance.getOutputFile()).thenReturn(outputFile);

        final FileTail tail = mock(FileTail.class);
        when(fileService.tail(same(outputFile), eq(10L), any())).thenReturn(tail);

        terrariaInstanceOutputService.trackInstance(instance);
        assertSame(tail, terrariaInstanceOutputService.getInstanceOutputTail(instance));
    }

    @Test
    void testGetInstanceOutputTail_notTracked() {
        final Exception exception = assertThrows(IllegalStateException.class,
                () -> terrariaInstanceOutputService.getInstanceOutputTail(makeInstance()));
        assertEquals("The output of instance " + INSTANCE_UUID + " is not being tracked", exception.getMessage());
    }

    @Test
    void testStopTrackingInstance() {
        final TerrariaInstanceEntity instance = makeInstance();
        terrariaInstanceOutputService.trackInstance(instance);
        assertTrue(terrariaInstanceOutputService.isTrackingInstance(instance));

        terrariaInstanceOutputService.stopTrackingInstance(instance);
        assertFalse(terrariaInstanceOutputService.isTrackingInstance(instance));
    }

    @Test
    void testStopTrackingInstance_notTracked() {
        terrariaInstanceOutputService.stopTrackingInstance(makeInstance());
    }

    @Test
    void testOnReadStarted() {
        final TerrariaInstanceEntity instance = makeInstance();
        terrariaInstanceOutputService.trackInstance(instance);
        verify(fileService).tail(any(), anyLong(), tailEventConsumerCaptor.capture());
        when(terrariaInstanceRepository.getOne(INSTANCE_ID)).thenReturn(instance);
        tailEventConsumerCaptor.getValue().onReadStarted();
        verify(terrariaInstanceRepository).getOne(INSTANCE_ID);
    }

    @Test
    void testTrack_bootUp() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.BOOTING_UP);
        final FileTailEventConsumer tail = track(instance);
        simulateText(tail, "Terraria ",
                "Server v1.3.5.3 - tModLoader v0.11.8.4\n\n1\t\tWorld1\n2\t\tWorld2\nn\tNew World\nd <number>Delete " +
                        "World\nm\t\tMods Menu\nb\t\tMod Browser\n\nChoose ", "");
        assertSame(TerrariaInstanceState.BOOTING_UP, instance.getState());
        assertEquals(Map.of(1, "World1", 2, "World2"), instance.getPendingOptions());
        assertEquals(Collections.emptyMap(), instance.getOptions());

        simulateText(tail, "World: ");
        assertSame(TerrariaInstanceState.WORLD_MENU, instance.getState());
        assertEquals(Collections.emptyMap(), instance.getPendingOptions());
        assertEquals(Map.of(1, "World1", 2, "World2"), instance.getOptions());
    }

    @Test
    void testTrack_goToModMenu() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.WORLD_MENU);
        final FileTailEventConsumer tail = track(instance);
        simulateText(tail, "a\n\n1\t\tMod (enabled)\n ", "\n\nType");
        assertSame(TerrariaInstanceState.WORLD_MENU, instance.getState());
        simulateText(tail, " a command: ");
        assertSame(TerrariaInstanceState.MOD_MENU, instance.getState());
    }

    @Test
    void testTrack_goFromModMenuToWorldMenu() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.MOD_MENU);
        final FileTailEventConsumer tail = track(instance);
        simulateText(tail, "Choose World: ");
        assertSame(TerrariaInstanceState.WORLD_MENU, instance.getState());
    }

    @Test
    void testTrack_options() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.BOOTING_UP);
        final FileTailEventConsumer tail = track(instance);
        simulateText(tail, "1\t\tOldOption1\n", "");
        assertEquals(Map.of(1, "OldOption1"), instance.getPendingOptions());

        simulateText(tail, "1\t\tOption1\n1\t\tOption1\n2\t\tOpt", "ion2\n");
        assertEquals(Map.of(1, "Option1", 2, "Option2"), instance.getPendingOptions());
        assertEquals(Collections.emptyMap(), instance.getOptions());
    }

    @Test
    void testTrack_options_flush() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.BOOTING_UP);
        instance.acknowledgeMenuOption(1, "Option1");
        instance.acknowledgeMenuOption(2, "Option2");

        final FileTailEventConsumer tail = track(instance);
        simulateText(tail, "Choose World: ");
        assertEquals(Collections.emptyMap(), instance.getPendingOptions());
        assertEquals(Map.of(1, "Option1", 2, "Option2"), instance.getOptions());
    }

    private static <T> List<T> iterableToList(final Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
    }

    private static String serializeEvents(final Iterable<TerrariaInstanceEventEntity> events) {
        return StreamSupport.stream(events.spliterator(), false)
                .map(event -> event.getType() + "<" + event.getText() + ">")
                .collect(Collectors.joining(" "));
    }

    @Test
    void testTrack_events() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.BOOTING_UP);
        final FileTailEventConsumer tail = track(instance);
        final String[] textParts = new String[]{"\t\tOption1\n2\t\tOpt", "ion2\n", "Choose World:   \n",
                "\n\nType a " + "command:", "a"};
        simulateText(tail, textParts);
        verify(terrariaInstanceEventRepository, times(5)).saveAll(instanceEventCaptor.capture());
        final List<Iterable<TerrariaInstanceEventEntity>> eventCalls = instanceEventCaptor.getAllValues();

        assertEquals(5, eventCalls.size());
        assertEquals("OUTPUT<\t\tOption1\n>", serializeEvents(eventCalls.get(0)));
        assertEquals("IMPORTANT_OUTPUT<2\t\tOption2> OUTPUT<\n>", serializeEvents(eventCalls.get(1)));
        assertEquals("IMPORTANT_OUTPUT<Choose World:   > OUTPUT<\n>", serializeEvents(eventCalls.get(2)));
        assertEquals("OUTPUT<\n\n> IMPORTANT_OUTPUT<Type a command:>", serializeEvents(eventCalls.get(3)));
        assertEquals("", serializeEvents(eventCalls.get(4)));
    }

    @Test
    void testTrack_mods() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.BOOTING_UP);
        instance.setModLoaderVersion("1.2.3");
        final FileTailEventConsumer tail = track(instance);
        simulateText(tail, "Loading: Mod v1\n", "Loading: ModLoader v1.2.3\n", "Loading: ModLoader v1.2.4\n",
                "Loading: ModWithoutVersion\n", "Loading: OtherMod v2\n");
        assertEquals(Set.of("Mod v1", "ModLoader v1.2.4", "OtherMod v2"), instance.getLoadedMods());
    }

    @Test
    void testTrack_mods_unloadingMods() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.BOOTING_UP);
        instance.setLoadedMods(Set.of("Mod v1"));

        simulateText(track(instance), "Unloading mods...\n");
        assertEquals(Collections.emptySet(), instance.getLoadedMods());
    }

    @Test
    void testTrack_mods_findingMods() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.BOOTING_UP);
        instance.setLoadedMods(Set.of("Mod v1"));

        simulateText(track(instance), "Finding Mods...\n");
        assertEquals(Collections.emptySet(), instance.getLoadedMods());
    }

    @Test
    void testTrack_mods_instantiatingMods() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.BOOTING_UP);
        instance.setLoadedMods(Set.of("Mod v1"));

        simulateText(track(instance), "Instantiating Mods...\n");
        assertEquals(Collections.emptySet(), instance.getLoadedMods());
    }

    @Test
    void testTrack_mods_unloadingMods_running() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.RUNNING);
        instance.setLoadedMods(Set.of("Mod v1"));

        simulateText(track(instance), "Unloading mods...\nUnloading mods...");
        assertEquals(Set.of("Mod v1"), instance.getLoadedMods());
    }

    @Test
    void testTrack_events_detailed() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.PASSWORD_PROMPT);
        final FileTailEventConsumer tail = track(instance);
        final String[] textParts = new String[]{"a: 5%\n" + "b\nc\n" + "d: 10%\ne: 1%"};
        simulateText(tail, textParts);
        verify(terrariaInstanceEventRepository).saveAll(instanceEventCaptor.capture());
        final List<TerrariaInstanceEventEntity> events = iterableToList(instanceEventCaptor.getValue());
        assertEquals("DETAILED_OUTPUT<a: 5%\n> OUTPUT<b\nc\n> DETAILED_OUTPUT<d: 10%\n>", serializeEvents(events));
    }

    @Test
    void testTrack_events_startingServer() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.PASSWORD_PROMPT);
        final FileTailEventConsumer tail = track(instance);
        final String[] textParts = new String[]{"Starting server...\n", "Server started\nRunning one update...\n\n\n"
                , "Listening on", " port 7777", "\n"};
        simulateText(tail, textParts);
        verify(terrariaInstanceEventRepository, times(5)).saveAll(instanceEventCaptor.capture());
        final List<Iterable<TerrariaInstanceEventEntity>> eventCalls = instanceEventCaptor.getAllValues();

        assertEquals(5, eventCalls.size());
        assertEquals("OUTPUT<Starting server...\n>", serializeEvents(eventCalls.get(0)));
        assertEquals("OUTPUT<Server started\nRunning one update...\n\n\n>", serializeEvents(eventCalls.get(1)));
        assertEquals("", serializeEvents(eventCalls.get(2)));
        assertEquals("IMPORTANT_OUTPUT<Listening on port 7777>", serializeEvents(eventCalls.get(3)));
        assertEquals("OUTPUT<\n>", serializeEvents(eventCalls.get(4)));
    }

    @Test
    void testTrack_createFile() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        instance.setLoadedMods(Set.of("Mod v1"));
        final FileTailEventConsumer tailEventConsumer = track(instance);
        when(tmuxService.hasSession(INSTANCE_UUID.toString())).thenReturn(true);

        tailEventConsumer.onFileCreated();
        verify(terrariaInstanceService).saveInstance(instance);
        assertSame(TerrariaInstanceState.BOOTING_UP, instance.getState());
        assertEquals(Collections.emptySet(), instance.getLoadedMods());

        verify(terrariaInstanceEventRepository).saveAll(instanceEventCaptor.capture());
        assertEquals("APPLICATION_START<>", serializeEvents(instanceEventCaptor.getValue()));
    }

    @Test
    void testTrack_createFile_unexpectedTmuxState()
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        final FileTailEventConsumer tailEventConsumer = track(instance);
        when(tmuxService.hasSession(INSTANCE_UUID.toString())).thenReturn(false);

        tailEventConsumer.onFileCreated();
        verify(terrariaInstanceService).saveInstance(instance);
        assertSame(TerrariaInstanceState.BROKEN, instance.getState());
        assertEquals(instance.getError(), "The output file has been created but the instance isn't running");
    }

    @Test
    void testTrack_createFile_unknownTmuxState()
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        final FileTailEventConsumer tailEventConsumer = track(instance);
        when(tmuxService.hasSession(INSTANCE_UUID.toString())).thenThrow(mock(ProcessFailureException.class));

        tailEventConsumer.onFileCreated();
        verify(terrariaInstanceService).saveInstance(instance);
        assertSame(TerrariaInstanceState.BROKEN, instance.getState());
        assertEquals(instance.getError(),
                "The output file has been created but it's unknown if the instance is running or not");

        verify(terrariaInstanceEventRepository).saveAll(instanceEventCaptor.capture());
        assertEquals("ERROR<" + instance.getError() + ">", serializeEvents(instanceEventCaptor.getValue()));
    }

    @Test
    void testTrack_createFile_interrupted()
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        final FileTailEventConsumer tailEventConsumer = track(instance);
        when(tmuxService.hasSession(INSTANCE_UUID.toString())).thenThrow(new InterruptedException());

        tailEventConsumer.onFileCreated();
        verify(terrariaInstanceService).saveInstance(instance);
        assertSame(TerrariaInstanceState.BROKEN, instance.getState());
        assertEquals(instance.getError(), "TSW has been interrupted");

        verify(terrariaInstanceEventRepository).saveAll(instanceEventCaptor.capture());
        assertEquals("TSW_INTERRUPTED<>", serializeEvents(instanceEventCaptor.getValue()));
    }

    @Test
    void testTrack_deleteFile() throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.WORLD_MENU);
        instance.setLoadedMods(Set.of("Mod v1"));
        final FileTailEventConsumer tailEventConsumer = track(instance);
        when(tmuxService.hasSession(INSTANCE_UUID.toString())).thenReturn(false);

        tailEventConsumer.onFileDeleted();
        verify(terrariaInstanceService).saveInstance(instance);
        assertSame(TerrariaInstanceState.IDLE, instance.getState());
        assertEquals(Collections.emptySet(), instance.getLoadedMods());

        verify(terrariaInstanceEventRepository).saveAll(instanceEventCaptor.capture());
        assertEquals("APPLICATION_END<>", serializeEvents(instanceEventCaptor.getValue()));
    }

    @Test
    void testTrack_deleteFile_unexpectedTmuxState()
            throws ProcessFailureException, ProcessTimeoutException, InterruptedException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.WORLD_MENU);
        final FileTailEventConsumer tailEventConsumer = track(instance);
        when(tmuxService.hasSession(INSTANCE_UUID.toString())).thenReturn(true);

        tailEventConsumer.onFileDeleted();
        verify(terrariaInstanceService).saveInstance(instance);
        assertSame(TerrariaInstanceState.BROKEN, instance.getState());
        assertEquals(instance.getError(), "The output file has been deleted but the instance is still running");
    }

    private FileTailEventConsumer track(final TerrariaInstanceEntity initialInstance) {
        terrariaInstanceOutputService.trackInstance(initialInstance);
        when(terrariaInstanceRepository.getOne(INSTANCE_ID)).thenReturn(initialInstance);
        when(terrariaInstanceService.saveInstance(initialInstance)).thenReturn(initialInstance);
        verify(fileService).tail(any(), anyLong(), tailEventConsumerCaptor.capture());
        return tailEventConsumerCaptor.getValue();
    }

    private void simulateText(final FileTailEventConsumer tailEventConsumer, final String... textParts) {
        long position = 0;
        for (final String textPart : textParts) {
            tailEventConsumer.onReadStarted();
            for (int i = 0; i < textPart.length(); i++) {
                tailEventConsumer.onCharacter(textPart.charAt(i), position++);
            }
            tailEventConsumer.onReadFinished(position);
        }
    }
}