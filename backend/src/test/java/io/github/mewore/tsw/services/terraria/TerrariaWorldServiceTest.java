package io.github.mewore.tsw.services.terraria;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
            when(worldInfo.readFile()).thenReturn(readResult);
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
        verify(terrariaWorldRepository).saveAll(Collections.emptyList());
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
        verify(terrariaWorldRepository).saveAll(savedWorldListCaptor.capture());
        assertEquals(1, savedWorldListCaptor.getValue().size());
        final TerrariaWorldEntity savedWorld = savedWorldListCaptor.getValue().get(0);
        assertEquals(WORLD_NAME, savedWorld.getName());
        assertEquals(1L, savedWorld.getLastModified().toEpochMilli());
        assertSame(worldFile, savedWorld.getFile());
        assertNull(savedWorld.getMods());
        assertSame(host, savedWorld.getHost());
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
    }

    @Test
    void testSetUp_changedWorld() throws IOException {
        final HostEntity host = mock(HostEntity.class);
        when(localHostService.getOrCreateHost()).thenReturn(host);

        final TerrariaWorldFileEntity worldFile = mock(TerrariaWorldFileEntity.class);
        final TerrariaWorldInfo worldInfo = makeWorldInfo(8L, worldFile);
        when(terrariaWorldFileService.getAllWorldInfo()).thenReturn(List.of(worldInfo));

        final TerrariaWorldEntity changedWorld = mock(TerrariaWorldEntity.class);
        when(changedWorld.getName()).thenReturn(WORLD_NAME);
        when(changedWorld.getLastModified()).thenReturn(Instant.ofEpochMilli(1L));
        when(terrariaWorldRepository.findByHost(host)).thenReturn(List.of(changedWorld));

        terrariaWorldService.setUp();
        verify(changedWorld).setLastModified(Instant.ofEpochMilli(8L));
        verify(changedWorld).updateFile(same(worldFile));
        verify(changedWorld).setMods(null);
        verify(terrariaWorldRepository).saveAll(List.of(changedWorld));
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
        verify(terrariaWorldRepository).saveAll(Collections.emptyList());
    }

    @Test
    void testUpdateWorld() throws IOException {
        final TerrariaWorldFileEntity worldFile = mock(TerrariaWorldFileEntity.class);
        final TerrariaWorldEntity world = mock(TerrariaWorldEntity.class);
        when(world.getName()).thenReturn(WORLD_NAME);
        when(world.getLastModified()).thenReturn(Instant.ofEpochMilli(1L));
        when(world.getFile()).thenReturn(worldFile);

        final TerrariaWorldInfo worldInfo = mock(TerrariaWorldInfo.class);
        final TerrariaWorldFileEntity readFileResult = mock(TerrariaWorldFileEntity.class);
        when(worldInfo.readFile()).thenReturn(readFileResult);
        when(worldInfo.getLastModified()).thenReturn(Instant.ofEpochMilli(8L));

        when(terrariaWorldFileService.getWorldInfo(WORLD_NAME)).thenReturn(worldInfo);

        terrariaWorldService.updateWorld(world, Set.of("Mod"));

        verify(world).setLastModified(Instant.ofEpochMilli(8L));
        verify(world).updateFile(same(readFileResult));
        verify(world).setMods(Set.of("Mod"));
        verify(terrariaWorldFileRepository, only()).save(same(worldFile));
        verify(terrariaWorldRepository, only()).save(same(world));
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
    void testUpdateWorld_IOException() throws IOException {
        final TerrariaWorldEntity world = mock(TerrariaWorldEntity.class);
        when(world.getName()).thenReturn(WORLD_NAME);

        final TerrariaWorldInfo worldInfo = mock(TerrariaWorldInfo.class);
        when(worldInfo.readFile()).thenThrow(new IOException());

        when(terrariaWorldFileService.getWorldInfo(WORLD_NAME)).thenReturn(worldInfo);

        terrariaWorldService.updateWorld(world, Collections.emptySet());
        verify(terrariaWorldRepository, never()).save(any());
    }

    @Test
    void testUpdateWorld_upToDate() throws IOException {
        final TerrariaWorldEntity world = mock(TerrariaWorldEntity.class);
        when(world.getName()).thenReturn(WORLD_NAME);
        when(world.getLastModified()).thenReturn(Instant.ofEpochMilli(1L));

        final TerrariaWorldInfo worldInfo = mock(TerrariaWorldInfo.class);
        when(worldInfo.readFile()).thenReturn(mock(TerrariaWorldFileEntity.class));
        when(worldInfo.getLastModified()).thenReturn(Instant.ofEpochMilli(1L));

        when(terrariaWorldFileService.getWorldInfo(WORLD_NAME)).thenReturn(worldInfo);

        terrariaWorldService.updateWorld(world, Collections.emptySet());
        verify(terrariaWorldRepository, never()).save(any());
    }
}