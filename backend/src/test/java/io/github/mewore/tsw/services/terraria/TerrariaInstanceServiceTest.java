package io.github.mewore.tsw.services.terraria;

import java.io.File;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import io.github.mewore.tsw.events.TerrariaInstanceUpdatedEvent;
import io.github.mewore.tsw.exceptions.InvalidRequestException;
import io.github.mewore.tsw.exceptions.NotFoundException;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceAction;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceRunServerModel;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldRepository;

import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.makeInstanceWithState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerrariaInstanceServiceTest {

    @InjectMocks
    private TerrariaInstanceService terrariaInstanceService;

    @Mock
    private TerrariaInstanceRepository terrariaInstanceRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private TerrariaWorldRepository terrariaWorldRepository;

    @Captor
    private ArgumentCaptor<ApplicationEvent> applicationEventCaptor;

    private static TerrariaInstanceEntity makeInstanceAtModMenu(final String... options) {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.WORLD_MENU);
        for (int i = 0; i < options.length; i++) {
            instance.acknowledgeMenuOption(i + 1, options[i]);
        }
        instance.setState(TerrariaInstanceState.MOD_MENU);
        return instance;
    }

    @Test
    void testGetInstance() throws NotFoundException {
        final TerrariaInstanceEntity instance = mock(TerrariaInstanceEntity.class);
        when(terrariaInstanceRepository.findById(1L)).thenReturn(Optional.of(instance));

        final TerrariaInstanceEntity result = terrariaInstanceService.getInstance(1L);
        assertSame(instance, result);
    }

    @Test
    void testGetInstance_notFound() {
        when(terrariaInstanceRepository.findById(1L)).thenReturn(Optional.empty());
        final Exception exception = assertThrows(NotFoundException.class,
                () -> terrariaInstanceService.getInstance(1L));
        assertEquals("Could not find a Terraria instance with ID 1", exception.getMessage());
    }

    @Test
    void testSaveInstance() {
        final TerrariaInstanceEntity instance = mock(TerrariaInstanceEntity.class);
        final TerrariaInstanceEntity savedInstance = mock(TerrariaInstanceEntity.class);
        when(terrariaInstanceRepository.save(instance)).thenReturn(savedInstance);

        final TerrariaInstanceEntity result = terrariaInstanceService.saveInstance(instance);
        assertSame(savedInstance, result);
        verify(applicationEventPublisher).publishEvent(applicationEventCaptor.capture());
        assertSame(savedInstance,
                ((TerrariaInstanceUpdatedEvent) applicationEventCaptor.getValue()).getChangedInstance());
    }

    @Test
    void testEnsureInstanceHasNoOutputFile() {
        final TerrariaInstanceEntity instance = mock(TerrariaInstanceEntity.class);
        final File outputFile = mock(File.class);
        when(instance.getOutputFile()).thenReturn(outputFile);
        when(outputFile.exists()).thenReturn(true);
        when(outputFile.delete()).thenReturn(true);
        terrariaInstanceService.ensureInstanceHasNoOutputFile(instance);
    }

    @Test
    void testEnsureInstanceHasNoOutputFile_nonExistent() {
        final TerrariaInstanceEntity instance = mock(TerrariaInstanceEntity.class);
        final File outputFile = mock(File.class);
        when(instance.getOutputFile()).thenReturn(outputFile);
        when(outputFile.exists()).thenReturn(false);
        terrariaInstanceService.ensureInstanceHasNoOutputFile(instance);
        verify(outputFile, never()).delete();
    }

    @Test
    void testEnsureInstanceHasNoOutputFile_failureToDelete() {
        final TerrariaInstanceEntity instance = mock(TerrariaInstanceEntity.class);
        final File outputFile = mock(File.class);
        when(instance.getOutputFile()).thenReturn(outputFile);
        when(outputFile.exists()).thenReturn(true);
        when(outputFile.delete()).thenReturn(false);
        when(outputFile.getAbsolutePath()).thenReturn("/path/to/file");

        final Exception exception = assertThrows(IllegalStateException.class,
                () -> terrariaInstanceService.ensureInstanceHasNoOutputFile(instance));
        assertEquals("Failed to delete file /path/to/file", exception.getMessage());
    }

    @Test
    public void testRequestActionForInstance() throws NotFoundException, InvalidRequestException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        when(terrariaInstanceRepository.findById(8L)).thenReturn(Optional.of(instance));
        when(terrariaInstanceRepository.save(instance)).thenReturn(instance);

        final TerrariaInstanceEntity result = terrariaInstanceService.requestActionForInstance(8L,
                TerrariaInstanceAction.BOOT_UP);
        assertSame(instance, result);
        assertSame(TerrariaInstanceAction.BOOT_UP, result.getPendingAction());
    }

    @Test
    public void testRequestActionForInstance_alreadyWithPendingAction() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.IDLE);
        instance.setPendingAction(TerrariaInstanceAction.DELETE);
        when(terrariaInstanceRepository.findById(anyLong())).thenReturn(Optional.of(instance));

        final Exception exception = assertThrows(InvalidRequestException.class,
                () -> terrariaInstanceService.requestActionForInstance(8L, TerrariaInstanceAction.BOOT_UP));
        assertEquals("Cannot apply an action to an instance that already has a pending action", exception.getMessage());
    }

    @Test
    public void testRequestActionForInstance_inapplicableAction() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.RUNNING);
        when(terrariaInstanceRepository.findById(anyLong())).thenReturn(Optional.of(instance));

        final Exception exception = assertThrows(InvalidRequestException.class,
                () -> terrariaInstanceService.requestActionForInstance(8L, TerrariaInstanceAction.BOOT_UP));
        assertEquals("Cannot apply action BOOT_UP to an instance with the state RUNNING", exception.getMessage());
    }

    @Test
    public void testRequestEnableInstanceMods() throws NotFoundException, InvalidRequestException {
        final TerrariaInstanceEntity instance = makeInstanceAtModMenu("Mod1 (enabled)");
        when(terrariaInstanceRepository.findById(8L)).thenReturn(Optional.of(instance));
        when(terrariaInstanceRepository.save(instance)).thenReturn(instance);

        final TerrariaInstanceEntity result = terrariaInstanceService.requestEnableInstanceMods(8L, Set.of("Mod1"));
        assertSame(instance, result);
        assertSame(TerrariaInstanceAction.SET_LOADED_MODS, result.getPendingAction());
    }

    @Test
    public void testRequestEnableInstanceMods_invalidModOption() {
        final TerrariaInstanceEntity instance = makeInstanceAtModMenu("Mod1 (disabled)");
        when(terrariaInstanceRepository.findById(anyLong())).thenReturn(Optional.of(instance));

        final Exception exception = assertThrows(InvalidRequestException.class,
                () -> terrariaInstanceService.requestEnableInstanceMods(8L, Set.of("Mod1", "Mod2", "Mod3")));
        assertEquals("Cannot enable the following mods because they aren't in the list of known options: Mod2, Mod3",
                exception.getMessage());
    }

    @Test
    public void testRequestEnableInstanceMods_exceptionWithNoMessage() {
        final TerrariaInstanceEntity instance = spy(makeInstanceAtModMenu("Mod1 (disabled)"));
        when(instance.getOptions()).thenThrow(new IllegalArgumentException());
        when(terrariaInstanceRepository.findById(anyLong())).thenReturn(Optional.of(instance));

        final Exception exception = assertThrows(InvalidRequestException.class,
                () -> terrariaInstanceService.requestEnableInstanceMods(8L, Set.of("Mod1", "Mod2", "Mod3")));
        assertEquals("Failed to map the requested enabled mods to the list of options", exception.getMessage());
    }

    @Test
    public void testRequestRunInstance() throws NotFoundException, InvalidRequestException {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.WORLD_MENU);
        when(terrariaInstanceRepository.findById(8L)).thenReturn(Optional.of(instance));
        when(terrariaInstanceRepository.save(instance)).thenReturn(instance);
        final TerrariaInstanceRunServerModel model = new TerrariaInstanceRunServerModel(10, 7777, false, "password",
                11L);

        final TerrariaWorldEntity world = mock(TerrariaWorldEntity.class);
        when(terrariaWorldRepository.findById(11L)).thenReturn(Optional.of(world));

        final TerrariaInstanceEntity result = terrariaInstanceService.requestRunInstance(8L, model);
        assertSame(instance, result);
        assertSame(TerrariaInstanceAction.RUN_SERVER, result.getPendingAction());
        assertEquals(10, result.getMaxPlayers());
        assertEquals(7777, result.getPort());
        assertEquals(false, result.getAutomaticallyForwardPort());
        assertEquals("password", result.getPassword());
        assertSame(world, result.getWorld());
    }

    @Test
    public void testRequestRunInstance_noWorld() {
        final TerrariaInstanceEntity instance = makeInstanceWithState(TerrariaInstanceState.WORLD_MENU);
        when(terrariaInstanceRepository.findById(8L)).thenReturn(Optional.of(instance));
        final TerrariaInstanceRunServerModel model = new TerrariaInstanceRunServerModel(10, 7777, false, "password",
                11L);

        when(terrariaWorldRepository.findById(11L)).thenReturn(Optional.empty());

        final Exception exception = assertThrows(InvalidRequestException.class,
                () -> terrariaInstanceService.requestRunInstance(8L, model));
        assertEquals("There is no world with ID 11", exception.getMessage());
    }

    @Test
    void testGetDesiredModOption_enable() {
        final TerrariaInstanceEntity instance = makeInstanceAtModMenu("Mod1 (disabled)", "Mod2 (disabled)");
        instance.setModsToEnable(Set.of("Mod2"));
        assertEquals(2, terrariaInstanceService.getDesiredModOption(instance));
    }

    @Test
    void testGetDesiredModOption_disable() {
        final TerrariaInstanceEntity instance = makeInstanceAtModMenu("Mod1 (disabled)", "Mod2 (enabled)");
        assertEquals(2, terrariaInstanceService.getDesiredModOption(instance));
    }

    @Test
    void testGetDesiredModOption_none() {
        final TerrariaInstanceEntity instance = makeInstanceAtModMenu("Mod1 (enabled)", "Mod2 (disabled)");
        instance.setModsToEnable(Set.of("Mod1"));

        assertNull(terrariaInstanceService.getDesiredModOption(instance));
    }

    @Test
    void testGetDesiredModOption_unselectableMod() {
        final TerrariaInstanceEntity instance = makeInstanceAtModMenu("Mod1 (enabled)", "Mod2 (enabled)");
        instance.setModsToEnable(Set.of("Mod1", "Mod3", "Mod4"));

        final Exception exception = assertThrows(IllegalArgumentException.class,
                () -> terrariaInstanceService.getDesiredModOption(instance));
        assertEquals("Cannot enable the following mods because they aren't in the list of known options: Mod3, Mod4",
                exception.getMessage());
    }
}