package io.github.mewore.tsw.services.terraria;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
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
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldRepository;
import io.github.mewore.tsw.services.LocalHostService;
import io.github.mewore.tsw.services.util.FileService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerrariaWorldServiceTest {

    private static final File TERRARIA_WORLD_DIRECTORY =
            Path.of(System.getProperty("user.home"), ".local", "share", "Terraria", "ModLoader", "Worlds").toFile();

    @InjectMocks
    private TerrariaWorldService terrariaWorldService;

    @Mock
    private LocalHostService localHostService;

    @Mock
    private FileService fileService;

    @Mock
    private TerrariaWorldRepository terrariaWorldRepository;

    @Captor
    private ArgumentCaptor<List<TerrariaWorldEntity>> terrariaWorldListCaptor;

    private static File makeFile(final String name, final long lastModified) {
        final File file = mock(File.class);
        when(file.getName()).thenReturn(name);
        when(file.lastModified()).thenReturn(lastModified);
        return file;
    }

    @Test
    void testSetUp() throws IOException {
        final HostEntity host = mock(HostEntity.class);
        when(localHostService.getOrCreateHost()).thenReturn(host);

        final File wldFile = makeFile("world.wld", 1);
        when(fileService.listFiles(TERRARIA_WORLD_DIRECTORY, "wld")).thenReturn(
                new File[]{new File("world-without-twld.wld"), wldFile});

        final File twldFile = makeFile("world.twld", 8);
        when(fileService.listFiles(TERRARIA_WORLD_DIRECTORY, "twld")).thenReturn(
                new File[]{new File("world-without-wld.twld"), twldFile});

        final byte[] zipData = new byte[]{1, 2, 3};
        when(fileService.zip(wldFile, twldFile)).thenReturn(zipData);

        terrariaWorldService.setUp();
        verify(terrariaWorldRepository).setHostWorlds(same(host), terrariaWorldListCaptor.capture());

        final TerrariaWorldEntity expectedSavedWorld = TerrariaWorldEntity.builder()
                .name("world")
                .lastModified(Instant.ofEpochMilli(8))
                .data(zipData)
                .host(host)
                .build();
        assertEquals(Collections.singletonList(expectedSavedWorld), terrariaWorldListCaptor.getValue());
    }
}