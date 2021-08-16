package io.github.mewore.tsw.services.terraria;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;
import io.github.mewore.tsw.models.terraria.TerrariaWorldFileEntity;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldFileRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldRepository;
import io.github.mewore.tsw.services.LocalHostService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerrariaWorldServiceTest {

    private static final String WORLD_NAME = "World";

    @InjectMocks
    private TerrariaWorldService terrariaWorldService;

    @Mock
    private LocalHostService localHostService;

    @Mock
    private TerrariaWorldFileService terrariaWorldFileService;

    @Mock
    private TerrariaWorldRepository terrariaWorldRepository;

    @Mock
    private TerrariaWorldFileRepository terrariaWorldFileRepository;

    @Captor
    private ArgumentCaptor<List<TerrariaWorldEntity>> savedWorldListCaptor;

    private static TerrariaWorldInfo makeWorldInfo(final long lastModified,
            final @Nullable TerrariaWorldFileEntity readResult) throws IOException {
        final TerrariaWorldInfo worldInfo = mock(TerrariaWorldInfo.class);
        when(worldInfo.getName()).thenReturn(WORLD_NAME);
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

        final TerrariaWorldEntity deletedWorld = mock(TerrariaWorldEntity.class);
        when(deletedWorld.getName()).thenReturn(WORLD_NAME);
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
        when(terrariaWorldFileService.getAllWorldInfo()).thenReturn(List.of(worldInfo));

        final TerrariaWorldEntity changedWorld = mock(TerrariaWorldEntity.class);
        when(changedWorld.getName()).thenReturn(WORLD_NAME);
        when(changedWorld.getLastModified()).thenReturn(Instant.ofEpochMilli(1L));
        when(terrariaWorldRepository.findByHost(host)).thenReturn(List.of(changedWorld));

        final TerrariaWorldEntity savedWorld = mock(TerrariaWorldEntity.class);
        when(terrariaWorldRepository.save(changedWorld)).thenReturn(savedWorld);

        final TerrariaWorldFileEntity existingWorldFile = mock(TerrariaWorldFileEntity.class);
        when(terrariaWorldFileRepository.findByWorld(savedWorld)).thenReturn(Optional.of(existingWorldFile));
        when(existingWorldFile.update(worldFileFromWorldInfo, savedWorld)).thenReturn(existingWorldFile);

        terrariaWorldService.setUp();

        verify(terrariaWorldRepository).save(changedWorld);
        verify(changedWorld).setLastModified(Instant.ofEpochMilli(8L));
        verify(changedWorld).setMods(null);
        verify(terrariaWorldFileRepository).save(existingWorldFile);
        verify(existingWorldFile).update(worldFileFromWorldInfo, savedWorld);
        verify(terrariaWorldFileRepository).saveAll(Collections.emptyList());
    }

    @Test
    void testSetUp_unchangedWorld() throws IOException {
        final HostEntity host = mock(HostEntity.class);
        when(localHostService.getOrCreateHost()).thenReturn(host);

        final TerrariaWorldInfo worldInfo = makeWorldInfo(8L, null);
        when(terrariaWorldFileService.getAllWorldInfo()).thenReturn(List.of(worldInfo));

        final TerrariaWorldEntity unchangedWorld = mock(TerrariaWorldEntity.class);
        when(unchangedWorld.getName()).thenReturn(WORLD_NAME);
        when(unchangedWorld.getLastModified()).thenReturn(Instant.ofEpochMilli(8L));
        when(terrariaWorldRepository.findByHost(host)).thenReturn(List.of(unchangedWorld));

        terrariaWorldService.setUp();
        verify(terrariaWorldFileRepository).saveAll(Collections.emptyList());
    }

    @Test
    void testUpdateWorld() throws IOException {
        final TerrariaWorldEntity world = mock(TerrariaWorldEntity.class);
        when(world.getName()).thenReturn(WORLD_NAME);
        when(world.getLastModified()).thenReturn(Instant.ofEpochMilli(1L));

        final TerrariaWorldInfo worldInfo = mock(TerrariaWorldInfo.class);
        final TerrariaWorldFileEntity readFileResult = mock(TerrariaWorldFileEntity.class);
        when(worldInfo.getLastModified()).thenReturn(Instant.ofEpochMilli(8L));


        when(terrariaWorldFileService.getWorldInfo(WORLD_NAME)).thenReturn(worldInfo);

        final TerrariaWorldEntity savedWorld = mock(TerrariaWorldEntity.class);
        when(terrariaWorldRepository.save(world)).thenReturn(savedWorld);
        when(worldInfo.readFile(savedWorld)).thenReturn(readFileResult);

        final TerrariaWorldFileEntity worldFile = mock(TerrariaWorldFileEntity.class);
        when(terrariaWorldFileRepository.findByWorld(savedWorld)).thenReturn(Optional.of(worldFile));
        when(worldFile.update(readFileResult, savedWorld)).thenReturn(worldFile);

        terrariaWorldService.updateWorld(world, Set.of("Mod"));

        verify(terrariaWorldRepository, only()).save(same(world));
        verify(world).setLastModified(Instant.ofEpochMilli(8L));
        verify(world).setMods(Set.of("Mod"));

        verify(terrariaWorldFileRepository).save(same(worldFile));
        verify(worldFile).update(same(readFileResult), same(savedWorld));
    }

    @Test
    void testUpdateWorld_missingWorld() {
        final TerrariaWorldEntity world = mock(TerrariaWorldEntity.class);
        when(world.getName()).thenReturn(WORLD_NAME);

        when(terrariaWorldFileService.getWorldInfo(WORLD_NAME)).thenReturn(null);

        terrariaWorldService.updateWorld(world, Collections.emptySet());
        verify(terrariaWorldRepository, never()).save(any());
    }

    @Test
    void testUpdateWorld_upToDate() {
        final TerrariaWorldEntity world = mock(TerrariaWorldEntity.class);
        when(world.getName()).thenReturn(WORLD_NAME);
        when(world.getLastModified()).thenReturn(Instant.ofEpochMilli(1L));

        final TerrariaWorldInfo worldInfo = mock(TerrariaWorldInfo.class);
        when(worldInfo.getLastModified()).thenReturn(Instant.ofEpochMilli(1L));

        when(terrariaWorldFileService.getWorldInfo(WORLD_NAME)).thenReturn(worldInfo);

        terrariaWorldService.updateWorld(world, Collections.emptySet());
        verify(terrariaWorldRepository, never()).save(any());
    }

    @Test
    void testUpdateWorld_IOException() throws IOException {
        final TerrariaWorldEntity world = mock(TerrariaWorldEntity.class);
        when(world.getName()).thenReturn(WORLD_NAME);
        when(world.getLastModified()).thenReturn(Instant.ofEpochMilli(1L));

        final TerrariaWorldInfo worldInfo = mock(TerrariaWorldInfo.class);
        when(worldInfo.getLastModified()).thenReturn(Instant.ofEpochMilli(8L));

        final TerrariaWorldEntity savedWorld = mock(TerrariaWorldEntity.class);
        when(terrariaWorldRepository.save(world)).thenReturn(savedWorld);
        when(worldInfo.readFile(savedWorld)).thenThrow(new IOException());

        when(terrariaWorldFileService.getWorldInfo(WORLD_NAME)).thenReturn(worldInfo);

        terrariaWorldService.updateWorld(world, Collections.emptySet());
        verify(terrariaWorldRepository, only()).save(same(world));
    }

    @Test
    void testUpdateWorld_noFile() throws IOException {
        final TerrariaWorldEntity world = mock(TerrariaWorldEntity.class);
        when(world.getName()).thenReturn(WORLD_NAME);
        when(world.getLastModified()).thenReturn(Instant.ofEpochMilli(1L));

        final TerrariaWorldInfo worldInfo = mock(TerrariaWorldInfo.class);
        final TerrariaWorldFileEntity readFileResult = mock(TerrariaWorldFileEntity.class);
        when(worldInfo.getLastModified()).thenReturn(Instant.ofEpochMilli(8L));

        final TerrariaWorldEntity savedWorld = mock(TerrariaWorldEntity.class);
        when(terrariaWorldRepository.save(world)).thenReturn(savedWorld);
        when(worldInfo.readFile(savedWorld)).thenReturn(readFileResult);

        when(terrariaWorldFileRepository.findByWorld(savedWorld)).thenReturn(Optional.empty());

        when(terrariaWorldFileService.getWorldInfo(WORLD_NAME)).thenReturn(worldInfo);

        terrariaWorldService.updateWorld(world, Set.of("Mod"));

        verify(terrariaWorldRepository, only()).save(same(world));
        verify(terrariaWorldFileRepository).save(same(readFileResult));
    }
}