package io.github.mewore.tsw.services.terraria;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;
import io.github.mewore.tsw.repositories.file.FileDataRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldRepository;
import io.github.mewore.tsw.services.LocalHostService;
import io.github.mewore.tsw.services.util.FileService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerrariaWorldServiceTest {

    private static final String WORLD_NAME = "world";

    private static final Path WORLD_DIRECTORY = Path.of(System.getProperty("user.home"), ".local", "share", "Terraria",
            "ModLoader", "Worlds");

    @InjectMocks
    private TerrariaWorldService terrariaWorldService;

    @Mock
    private LocalHostService localHostService;

    @Mock
    private FileService fileService;

    @Mock
    private TerrariaWorldRepository terrariaWorldRepository;

    @Mock
    private FileDataRepository fileDataRepository;

    @Captor
    private ArgumentCaptor<List<TerrariaWorldEntity>> terrariaWorldListCaptor;

    private static File makeFile(final String name) {
        final File file = mock(File.class);
        when(file.getName()).thenReturn(name);
        when(file.exists()).thenReturn(true);
        return file;
    }

    private static TerrariaWorldEntity makeWorldMock() {
        final HostEntity host = mock(HostEntity.class);
        final TerrariaWorldEntity world = mock(TerrariaWorldEntity.class);
        when(world.getName()).thenReturn(WORLD_NAME);
        when(world.getHost()).thenReturn(host);
        return world;
    }

    @Test
    void testSetUp() throws IOException {
        final HostEntity host = mock(HostEntity.class);
        when(localHostService.getOrCreateHost()).thenReturn(host);

        final File wldFile = makeFile("world.wld");
        when(wldFile.lastModified()).thenReturn(1L);
        final File twldFile = mock(File.class);
        when(twldFile.exists()).thenReturn(true);
        when(twldFile.lastModified()).thenReturn(8L);
        when(fileService.pathToFile(WORLD_DIRECTORY.resolve("world.wld"))).thenReturn(wldFile);
        when(fileService.pathToFile(WORLD_DIRECTORY.resolve("world.twld"))).thenReturn(twldFile);

        final File wldFileWithoutTwld = makeFile("world-without-twld.wld");
        when(fileService.pathToFile(WORLD_DIRECTORY.resolve("world-without-twld.wld"))).thenReturn(wldFileWithoutTwld);
        final File nonExistentTwldFile = mock(File.class);
        when(nonExistentTwldFile.exists()).thenReturn(false);
        when(fileService.pathToFile(WORLD_DIRECTORY.resolve("world-without-twld.twld"))).thenReturn(
                nonExistentTwldFile);

        when(fileService.listFilesWithExtensions(WORLD_DIRECTORY.toFile(), "wld")).thenReturn(
                new File[]{wldFile, wldFileWithoutTwld});

        final byte[] zipData = new byte[]{1, 2, 3};
        when(fileService.zip(wldFile, twldFile)).thenReturn(zipData);

        terrariaWorldService.setUp();
        verify(terrariaWorldRepository).setHostWorlds(same(host), terrariaWorldListCaptor.capture());

        assertEquals(1, terrariaWorldListCaptor.getValue().size());
        final TerrariaWorldEntity savedWorld = terrariaWorldListCaptor.getValue().get(0);
        assertEquals("world", savedWorld.getName());
        assertEquals(Instant.ofEpochMilli(8L), savedWorld.getLastModified());
        assertEquals("world.zip", savedWorld.getData().getName());
        assertSame(zipData, savedWorld.getData().getContent());
        assertSame(host, savedWorld.getHost());
    }

    @Test
    void testReadWorld() throws IOException {
        final TerrariaWorldEntity world = makeWorldMock();

        final File wldFile = mock(File.class);
        when(wldFile.exists()).thenReturn(true);
        when(wldFile.lastModified()).thenReturn(1L);
        final File twldFile = mock(File.class);
        when(twldFile.exists()).thenReturn(true);
        when(twldFile.lastModified()).thenReturn(8L);
        when(fileService.pathToFile(WORLD_DIRECTORY.resolve("world.wld"))).thenReturn(wldFile);
        when(fileService.pathToFile(WORLD_DIRECTORY.resolve("world.twld"))).thenReturn(twldFile);

        final byte[] zipData = new byte[0];
        when(fileService.zip(wldFile, twldFile)).thenReturn(zipData);

        final TerrariaWorldEntity result = terrariaWorldService.readWorld(world);
        assertNotNull(result);
        assertSame(WORLD_NAME, result.getName());
        assertEquals(Instant.ofEpochMilli(8L), result.getLastModified());
        assertEquals("world.zip", result.getData().getName());
        assertSame(zipData, result.getData().getContent());
    }

    @Test
    void testReadWorld_missingWldFile() {
        final TerrariaWorldEntity world = makeWorldMock();

        final File wldFile = mock(File.class);
        when(wldFile.exists()).thenReturn(false);
        final File twldFile = mock(File.class);
        when(fileService.pathToFile(WORLD_DIRECTORY.resolve("world.wld"))).thenReturn(wldFile);
        when(fileService.pathToFile(WORLD_DIRECTORY.resolve("world.twld"))).thenReturn(twldFile);

        assertNull(terrariaWorldService.readWorld(world));
        verify(twldFile, never()).exists();
    }

    @Test
    void testReadWorld_missingTwldFile() {
        final TerrariaWorldEntity world = makeWorldMock();

        final File wldFile = mock(File.class);
        when(wldFile.exists()).thenReturn(true);
        final File twldFile = mock(File.class);
        when(twldFile.exists()).thenReturn(false);
        when(fileService.pathToFile(WORLD_DIRECTORY.resolve("world.wld"))).thenReturn(wldFile);
        when(fileService.pathToFile(WORLD_DIRECTORY.resolve("world.twld"))).thenReturn(twldFile);

        assertNull(terrariaWorldService.readWorld(world));
    }

    @Test
    void testReadWorld_IOException() throws IOException {
        final TerrariaWorldEntity world = makeWorldMock();

        final File wldFile = mock(File.class);
        when(wldFile.exists()).thenReturn(true);
        final File twldFile = mock(File.class);
        when(twldFile.exists()).thenReturn(true);
        when(fileService.pathToFile(WORLD_DIRECTORY.resolve("world.wld"))).thenReturn(wldFile);
        when(fileService.pathToFile(WORLD_DIRECTORY.resolve("world.twld"))).thenReturn(twldFile);

        when(fileService.zip(wldFile, twldFile)).thenThrow(new IOException("oof"));
        assertNull(terrariaWorldService.readWorld(world));
    }
}