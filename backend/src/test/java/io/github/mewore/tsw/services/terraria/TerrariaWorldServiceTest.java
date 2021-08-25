package io.github.mewore.tsw.services.terraria;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import io.github.mewore.tsw.events.TerrariaWorldDeletionEvent;
import io.github.mewore.tsw.exceptions.InvalidRequestException;
import io.github.mewore.tsw.exceptions.NotFoundException;
import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.terraria.world.TerrariaWorldEntity;
import io.github.mewore.tsw.models.terraria.world.TerrariaWorldFileEntity;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldFileRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldRepository;
import io.github.mewore.tsw.services.LocalHostService;

import static io.github.mewore.tsw.models.terraria.TerrariaWorldFactory.makeWorld;
import static io.github.mewore.tsw.models.terraria.TerrariaWorldFactory.makeWorldBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerrariaWorldServiceTest {

    private static final String WORLD_FILE_NAME = "Some_World";

    private static final String WORLD_NAME = "Some World";

    @InjectMocks
    private TerrariaWorldService terrariaWorldService;

    @Mock
    private LocalHostService localHostService;

    @Mock
    private TerrariaWorldFileService terrariaWorldFileService;

    @Mock
    private TerrariaInstanceRepository terrariaInstanceRepository;

    @Mock
    private TerrariaWorldRepository terrariaWorldRepository;

    @Mock
    private TerrariaWorldFileRepository terrariaWorldFileRepository;

    @Mock
    private TerrariaWorldDbNotificationService terrariaWorldDbNotificationService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Captor
    private ArgumentCaptor<TerrariaWorldDeletionEvent> deletionEventCaptor;

    private static TerrariaWorldInfo makeWorldInfo(final long lastModified, final TerrariaWorldFileEntity readResult)
            throws IOException {
        final TerrariaWorldInfo worldInfo = mock(TerrariaWorldInfo.class);
        when(worldInfo.getFileName()).thenReturn(WORLD_FILE_NAME);
        when(worldInfo.getDisplayName()).thenReturn(WORLD_NAME);
        when(worldInfo.getLastModified()).thenReturn(Instant.ofEpochMilli(lastModified));
        if (readResult != null) {
            when(worldInfo.readFile(any())).thenReturn(readResult);
        }
        return worldInfo;
    }

    @Test
    void testSetUp() throws IOException {
        final HostEntity host = mock(HostEntity.class);
        when(localHostService.getOrCreateHost()).thenReturn(host);
        when(terrariaWorldFileService.getAllWorldInfo()).thenReturn(Collections.emptyList());
        when(terrariaWorldRepository.findByHost(host)).thenReturn(Collections.emptyList());

        terrariaWorldService.setUp();
        verify(terrariaWorldFileRepository).saveAll(Collections.emptyList());
    }

    @Test
    void testSetUp_newWorld() throws IOException {
        final HostEntity host = mock(HostEntity.class);
        when(localHostService.getOrCreateHost()).thenReturn(host);

        final TerrariaWorldFileEntity worldFile = mock(TerrariaWorldFileEntity.class);
        final TerrariaWorldInfo worldInfo = makeWorldInfo(1L, worldFile);
        when(terrariaWorldFileService.getAllWorldInfo()).thenReturn(List.of(worldInfo));

        when(terrariaWorldRepository.findByHost(host)).thenReturn(Collections.emptyList());

        terrariaWorldService.setUp();
        verify(terrariaWorldFileRepository).saveAll(List.of(worldFile));
    }

    @Test
    void testSetUp_deletedWorld() throws IOException {
        final HostEntity host = mock(HostEntity.class);
        when(localHostService.getOrCreateHost()).thenReturn(host);
        when(terrariaWorldFileService.getAllWorldInfo()).thenReturn(Collections.emptyList());

        final TerrariaWorldEntity deletedWorld = makeWorldBuilder().fileName(WORLD_FILE_NAME).build();
        when(terrariaWorldRepository.findByHost(host)).thenReturn(List.of(deletedWorld));

        terrariaWorldService.setUp();
        verify(terrariaWorldFileService).recreateWorld(deletedWorld);
        verify(terrariaWorldFileRepository).saveAll(Collections.emptyList());
    }

    @Test
    void testSetUp_changedWorld() throws IOException {
        final HostEntity host = mock(HostEntity.class);
        when(localHostService.getOrCreateHost()).thenReturn(host);

        final TerrariaWorldFileEntity worldFileFromWorldInfo = mock(TerrariaWorldFileEntity.class);
        final TerrariaWorldInfo worldInfo = makeWorldInfo(8L, worldFileFromWorldInfo);
        when(worldInfo.getDisplayName()).thenReturn("New World Name");
        when(terrariaWorldFileService.getAllWorldInfo()).thenReturn(List.of(worldInfo));

        final TerrariaWorldEntity changedWorld = makeWorldBuilder().fileName(WORLD_FILE_NAME)
                .lastModified(Instant.ofEpochMilli(1L))
                .build();
        when(terrariaWorldRepository.findByHost(host)).thenReturn(List.of(changedWorld));

        final TerrariaWorldEntity savedWorld = makeWorld();
        when(terrariaWorldRepository.save(changedWorld)).thenReturn(savedWorld);

        final TerrariaWorldFileEntity existingWorldFile = mock(TerrariaWorldFileEntity.class);
        when(terrariaWorldFileRepository.findByWorld(savedWorld)).thenReturn(Optional.of(existingWorldFile));
        when(existingWorldFile.update(worldFileFromWorldInfo, savedWorld)).thenReturn(existingWorldFile);

        terrariaWorldService.setUp();

        verify(terrariaWorldRepository).save(changedWorld);
        assertEquals("New World Name", changedWorld.getDisplayName());
        assertEquals(Instant.ofEpochMilli(8L), changedWorld.getLastModified());
        assertNull(changedWorld.getMods());
        verify(terrariaWorldFileRepository).save(existingWorldFile);
        verify(existingWorldFile).update(worldFileFromWorldInfo, savedWorld);
        verify(terrariaWorldFileRepository).saveAll(Collections.emptyList());
    }

    @Test
    void testSetUp_unchangedWorld() throws IOException {
        final HostEntity host = mock(HostEntity.class);
        when(localHostService.getOrCreateHost()).thenReturn(host);

        final TerrariaWorldInfo worldInfo = mock(TerrariaWorldInfo.class);
        when(worldInfo.getFileName()).thenReturn(WORLD_FILE_NAME);
        when(worldInfo.getLastModified()).thenReturn(Instant.ofEpochMilli(8L));
        when(terrariaWorldFileService.getAllWorldInfo()).thenReturn(List.of(worldInfo));

        final TerrariaWorldEntity unchangedWorld = makeWorldBuilder().fileName(WORLD_FILE_NAME)
                .displayName(WORLD_NAME)
                .lastModified(Instant.ofEpochMilli(8L))
                .build();
        when(terrariaWorldRepository.findByHost(host)).thenReturn(List.of(unchangedWorld));

        terrariaWorldService.setUp();
        verify(terrariaWorldFileRepository).saveAll(Collections.emptyList());
    }

    @Test
    void testGetWorldData() throws NotFoundException {
        final TerrariaWorldFileEntity file = mock(TerrariaWorldFileEntity.class);
        when(terrariaWorldFileRepository.findById(1L)).thenReturn(Optional.of(file));
        assertSame(file, terrariaWorldService.getWorldData(1L));
    }

    @Test
    void testGetWorldData_notFound() {
        when(terrariaWorldFileRepository.findById(1L)).thenReturn(Optional.empty());
        final Exception exception = assertThrows(NotFoundException.class, () -> terrariaWorldService.getWorldData(1L));
        assertEquals("There is no file data for the world with ID 1", exception.getMessage());
    }

    @Test
    void testUpdateWorld() throws IOException {
        final TerrariaWorldEntity world = makeWorldBuilder().lastModified(Instant.ofEpochMilli(1L)).build();

        final TerrariaWorldInfo worldInfo = mock(TerrariaWorldInfo.class);
        final TerrariaWorldFileEntity readFileResult = mock(TerrariaWorldFileEntity.class);
        when(worldInfo.getDisplayName()).thenReturn("New World Name");
        when(worldInfo.getLastModified()).thenReturn(Instant.ofEpochMilli(8L));

        when(terrariaWorldFileService.getWorldInfo(same(world))).thenReturn(worldInfo);

        final TerrariaWorldEntity savedWorld = makeWorld();
        when(terrariaWorldRepository.save(world)).thenReturn(savedWorld);
        when(worldInfo.readFile(savedWorld)).thenReturn(readFileResult);

        final TerrariaWorldFileEntity worldFile = mock(TerrariaWorldFileEntity.class);
        when(terrariaWorldFileRepository.findByWorld(savedWorld)).thenReturn(Optional.of(worldFile));
        when(worldFile.update(readFileResult, savedWorld)).thenReturn(worldFile);

        terrariaWorldService.updateWorld(world, Set.of("Mod"));

        verify(terrariaWorldRepository, only()).save(same(world));
        assertEquals("New World Name", world.getDisplayName());
        assertEquals(Instant.ofEpochMilli(8L), world.getLastModified());
        assertEquals(Set.of("Mod"), world.getMods());

        verify(terrariaWorldFileRepository).save(same(worldFile));
        verify(worldFile).update(same(readFileResult), same(savedWorld));
    }

    @Test
    void testUpdateWorld_missingWorld() {
        final TerrariaWorldEntity world = makeWorldBuilder().displayName(WORLD_NAME).build();
        when(terrariaWorldFileService.getWorldInfo(same(world))).thenReturn(null);

        terrariaWorldService.updateWorld(world, Collections.emptySet());
        verify(terrariaWorldRepository, never()).save(any());
    }

    @Test
    void testUpdateWorld_upToDate() {
        final TerrariaWorldEntity world = makeWorldBuilder().lastModified(Instant.ofEpochMilli(1L)).build();

        final TerrariaWorldInfo worldInfo = mock(TerrariaWorldInfo.class);
        when(worldInfo.getLastModified()).thenReturn(Instant.ofEpochMilli(1L));

        when(terrariaWorldFileService.getWorldInfo(same(world))).thenReturn(worldInfo);

        terrariaWorldService.updateWorld(world, Collections.emptySet());
        verify(terrariaWorldRepository, never()).save(any());
    }

    @Test
    void testUpdateWorld_IOException() throws IOException {
        final TerrariaWorldEntity world = makeWorldBuilder().lastModified(Instant.ofEpochMilli(1L)).build();

        final TerrariaWorldInfo worldInfo = mock(TerrariaWorldInfo.class);
        when(worldInfo.getDisplayName()).thenReturn("New World Name");
        when(worldInfo.getLastModified()).thenReturn(Instant.ofEpochMilli(8L));

        final TerrariaWorldEntity savedWorld = makeWorld();
        when(terrariaWorldRepository.save(world)).thenReturn(savedWorld);
        when(worldInfo.readFile(savedWorld)).thenThrow(new IOException());

        when(terrariaWorldFileService.getWorldInfo(same(world))).thenReturn(worldInfo);

        terrariaWorldService.updateWorld(world, Collections.emptySet());
        assertEquals("New World Name", world.getDisplayName());
        verify(terrariaWorldRepository, only()).save(same(world));
    }

    @Test
    void testUpdateWorld_noFile() throws IOException {
        final TerrariaWorldEntity world = makeWorldBuilder().lastModified(Instant.ofEpochMilli(1L)).build();

        final TerrariaWorldInfo worldInfo = mock(TerrariaWorldInfo.class);
        final TerrariaWorldFileEntity readFileResult = mock(TerrariaWorldFileEntity.class);
        when(worldInfo.getDisplayName()).thenReturn("New World Name");
        when(worldInfo.getLastModified()).thenReturn(Instant.ofEpochMilli(8L));

        final TerrariaWorldEntity savedWorld = makeWorld();
        when(terrariaWorldRepository.save(world)).thenReturn(savedWorld);
        when(worldInfo.readFile(same(savedWorld))).thenReturn(readFileResult);

        when(terrariaWorldFileRepository.findByWorld(savedWorld)).thenReturn(Optional.empty());
        when(terrariaWorldFileService.getWorldInfo(same(world))).thenReturn(worldInfo);

        terrariaWorldService.updateWorld(world, Set.of("Mod"));

        verify(terrariaWorldRepository, only()).save(same(world));
        verify(terrariaWorldFileRepository).save(same(readFileResult));
    }

    @Test
    void testDeleteWorld() throws NotFoundException, InvalidRequestException {
        final TerrariaWorldEntity world = makeWorld();
        when(terrariaWorldRepository.findById(1L)).thenReturn(Optional.of(world));
        when(terrariaInstanceRepository.existsByWorld(same(world))).thenReturn(false);

        terrariaWorldService.deleteWorld(1L);
        verify(terrariaWorldRepository).delete(world);
        verify(applicationEventPublisher, only()).publishEvent(deletionEventCaptor.capture());
        assertSame(world.getId(), deletionEventCaptor.getValue().getDeletedWorld().getId());
        verify(terrariaWorldDbNotificationService, only()).worldDeleted(world);
    }

    @Test
    void testDeleteWorld_notFound() {
        when(terrariaWorldRepository.findById(1L)).thenReturn(Optional.empty());
        final Exception exception = assertThrows(NotFoundException.class, () -> terrariaWorldService.deleteWorld(1L));
        assertEquals("There is no world with ID 1", exception.getMessage());
    }

    @Test
    void testDeleteWorld_used() {
        final TerrariaWorldEntity world = makeWorld();
        when(terrariaWorldRepository.findById(1L)).thenReturn(Optional.of(world));
        when(terrariaInstanceRepository.existsByWorld(world)).thenReturn(true);

        final Exception exception = assertThrows(InvalidRequestException.class,
                () -> terrariaWorldService.deleteWorld(1L));
        assertEquals("World \"World Name\" with ID 1 is used by one or more instances. Stop the instances first.",
                exception.getMessage());
    }
}