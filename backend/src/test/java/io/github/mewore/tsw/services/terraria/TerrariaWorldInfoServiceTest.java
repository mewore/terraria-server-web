package io.github.mewore.tsw.services.terraria;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mewore.tsw.models.terraria.TerrariaWorldFileEntity;
import io.github.mewore.tsw.services.util.FileService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerrariaWorldInfoServiceTest {

    private static final Path TERRARIA_WORLD_DIRECTORY = Path.of(System.getProperty("user.home"), ".local", "share",
            "Terraria", "ModLoader", "Worlds");

    @InjectMocks
    private TerrariaWorldInfoService terrariaWorldInfoService;

    @Mock
    private FileService fileService;

    private static File[] files(final String... names) {
        return Arrays.stream(names).map(name -> {
            final File file = mock(File.class);
            when(file.getName()).thenReturn(name);
            return file;
        }).toArray(File[]::new);
    }

    private static File existingFile() {
        return existingFile(1L);
    }

    private static File existingFile(final long lastModified) {
        final File file = mock(File.class);
        when(file.exists()).thenReturn(true);
        when(file.lastModified()).thenReturn(lastModified);
        return file;
    }

    @Test
    void testGetAllWorldInfo() throws IOException {
        final File[] wldFiles = files("Present.wld", "Partial.wld");
        when(fileService.listFilesWithExtensions(TERRARIA_WORLD_DIRECTORY.toFile(), "wld")).thenReturn(wldFiles);

        final File presentWld = existingFile();
        final File presentTwld = existingFile();
        when(fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve("Present.wld"))).thenReturn(presentWld);
        when(fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve("Present.twld"))).thenReturn(presentTwld);

        final File partialWld = mock(File.class);
        when(partialWld.exists()).thenReturn(false);
        when(fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve("Partial.wld"))).thenReturn(partialWld);
        when(fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve("Partial.twld"))).thenReturn(mock(File.class));

        final List<TerrariaWorldInfo> result = terrariaWorldInfoService.getAllWorldInfo();
        assertEquals(1, result.size());

        when(fileService.zip(presentWld, presentTwld)).thenReturn("Content".getBytes());
        final TerrariaWorldFileEntity file = result.get(0).readFile();
        assertEquals("Content", new String(file.getContent()));
    }

    @Test
    void testGetWorldInfo() throws IOException {
        final File wldFile = existingFile(1L);
        final File twldFile = existingFile(8L);
        when(fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve("World.wld"))).thenReturn(wldFile);
        when(fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve("World.twld"))).thenReturn(twldFile);

        final @Nullable TerrariaWorldInfo result = terrariaWorldInfoService.getWorldInfo("World");
        assertNotNull(result);
        assertEquals("World", result.getName());
        assertEquals(8L, result.getLastModified().toEpochMilli());

        when(fileService.zip(wldFile, twldFile)).thenReturn("Content".getBytes());
        final TerrariaWorldFileEntity file = result.readFile();
        assertEquals("Content", new String(file.getContent()));
        assertEquals("World.zip", file.getName());
    }

    @Test
    void testGetWorldInfo_noWldFile() {
        final File partialWld = mock(File.class);
        when(partialWld.exists()).thenReturn(false);
        when(fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve("NoWld.wld"))).thenReturn(partialWld);
        when(fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve("NoWld.twld"))).thenReturn(mock(File.class));

        assertNull(terrariaWorldInfoService.getWorldInfo("NoWld"));
    }

    @Test
    void testGetWorldInfo_noTwldFile() {
        final File partialWld = mock(File.class);
        when(partialWld.exists()).thenReturn(true);
        final File partialTwld = mock(File.class);
        when(partialTwld.exists()).thenReturn(false);
        when(fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve("NoTwld.wld"))).thenReturn(partialWld);
        when(fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve("NoTwld.twld"))).thenReturn(partialTwld);

        assertNull(terrariaWorldInfoService.getWorldInfo("NoTwld"));
    }
}