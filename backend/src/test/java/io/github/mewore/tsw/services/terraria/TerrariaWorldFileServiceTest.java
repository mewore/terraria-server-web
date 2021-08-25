package io.github.mewore.tsw.services.terraria;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mewore.tsw.models.terraria.world.TerrariaWorldEntity;
import io.github.mewore.tsw.models.terraria.world.TerrariaWorldFileEntity;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldFileRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldRepository;
import io.github.mewore.tsw.services.util.FileService;

import static io.github.mewore.tsw.models.terraria.TerrariaWorldFactory.makeWorld;
import static io.github.mewore.tsw.models.terraria.TerrariaWorldFactory.makeWorldBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerrariaWorldFileServiceTest {

    private static final Path TERRARIA_WORLD_DIRECTORY = Path.of(System.getProperty("user.home"), ".local", "share",
            "Terraria", "ModLoader", "Worlds");

    @InjectMocks
    private TerrariaWorldFileService terrariaWorldFileService;

    @Mock
    private FileService fileService;

    @Mock
    private TerrariaWorldRepository terrariaWorldRepository;

    @Mock
    private TerrariaWorldFileRepository terrariaWorldFileRepository;

    @Captor
    private ArgumentCaptor<InputStream> inputStreamCaptor;

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

    private static TerrariaWorldEntity worldWithName(final String name) {
        return makeWorldBuilder().displayName(name).build();
    }

    private static InputStream wldDataForWorldName(final String name) {
        return wldDataForWorldName(name, name.length());
    }

    private static InputStream wldDataForWorldName(final String name, final int nameLength) {
        final byte[] result = new byte[0x7F + 1 + nameLength];
        result[0x7F] = (byte) name.length();
        System.arraycopy(name.getBytes(StandardCharsets.US_ASCII), 0, result, 0x7F + 1, nameLength);
        return new ByteArrayInputStream(result);
    }

    @Test
    void testGetAllWorldInfo() throws IOException {
        final File[] wldFiles = files("World_With_Underscores.wld");
        when(fileService.listFilesWithExtensions(TERRARIA_WORLD_DIRECTORY.toFile(), "wld")).thenReturn(wldFiles);

        final File wld = existingFile();
        final File twld = existingFile();
        when(fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve("World_With_Underscores.wld"))).thenReturn(wld);
        when(fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve("World_With_Underscores.twld"))).thenReturn(twld);

        when(fileService.readFileInStream(wld)).thenReturn(wldDataForWorldName("World With_Underscores"));

        final List<TerrariaWorldInfo> result = terrariaWorldFileService.getAllWorldInfo();
        assertEquals(1, result.size());
        assertEquals("World_With_Underscores", result.get(0).getFileName());
        assertEquals("World With_Underscores", result.get(0).getDisplayName());
        assertEquals(1L, result.get(0).getLastModified().toEpochMilli());

        when(fileService.zip(wld, twld)).thenReturn("Content".getBytes());

        final TerrariaWorldFileEntity file = result.get(0).readFile(makeWorld());
        assertEquals("Content", new String(file.getContent()));
    }

    @Test
    void testGetAllWorldInfo_empty() throws IOException {
        mockWorldWithData("Empty_world", new ByteArrayInputStream(new byte[0]));

        final TerrariaWorldInfo result = terrariaWorldFileService.getAllWorldInfo().get(0);
        assertEquals("Empty world", result.getDisplayName());
    }

    @Test
    void testGetAllWorldInfo_noLengthByte() throws IOException {
        mockWorldWithData("World_with_no_length_byte", new ByteArrayInputStream(new byte[0x7F]));

        final TerrariaWorldInfo result = terrariaWorldFileService.getAllWorldInfo().get(0);
        assertEquals("World with no length byte", result.getDisplayName());
    }

    @Test
    void testGetAllWorldInfo_mismatchingLength() throws IOException {
        final InputStream inputStream = wldDataForWorldName("Other name");
        mockWorldWithData("World_with_mismatching_length", inputStream);

        final TerrariaWorldInfo result = terrariaWorldFileService.getAllWorldInfo().get(0);
        assertEquals("World with mismatching length", result.getDisplayName());
    }

    @Test
    void testGetAllWorldInfo_incompleteName() throws IOException {
        final InputStream inputStream = wldDataForWorldName("World with incomplete name", 8);
        mockWorldWithData("World_with_incomplete_name", inputStream);

        final TerrariaWorldInfo result = terrariaWorldFileService.getAllWorldInfo().get(0);
        assertEquals("World with incomplete name", result.getDisplayName());
    }

    @Test
    void testGetAllWorldInfo_controlCharacters() throws IOException {
        final InputStream inputStream = wldDataForWorldName(new String(new byte[]{1, 1, 1, 1, 1}));
        mockWorldWithData("O_w_O", inputStream);

        final TerrariaWorldInfo result = terrariaWorldFileService.getAllWorldInfo().get(0);
        assertEquals("O w O", result.getDisplayName());
    }

    @Test
    void testGetAllWorldInfo_IOException() throws IOException {
        final InputStream inputStream = mock(InputStream.class);
        when(inputStream.skip(anyLong())).thenThrow(new IOException("oof"));
        mockWorldWithData("O_w_O", inputStream);

        final TerrariaWorldInfo result = terrariaWorldFileService.getAllWorldInfo().get(0);
        assertEquals("O w O", result.getDisplayName());
    }

    @Test
    void testGetAllWorldInfo_partial() {
        final File[] wldFiles = files("Partial.wld");
        when(fileService.listFilesWithExtensions(TERRARIA_WORLD_DIRECTORY.toFile(), "wld")).thenReturn(wldFiles);

        final File partialWld = mock(File.class);
        when(partialWld.exists()).thenReturn(false);
        when(fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve("Partial.wld"))).thenReturn(partialWld);
        when(fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve("Partial.twld"))).thenReturn(mock(File.class));

        final List<TerrariaWorldInfo> result = terrariaWorldFileService.getAllWorldInfo();
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetWorldInfo() throws IOException {
        final File wldFile = existingFile(1L);
        final File twldFile = existingFile(8L);
        when(fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve("Some_World.wld"))).thenReturn(wldFile);
        when(fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve("Some_World.twld"))).thenReturn(twldFile);

        final @Nullable TerrariaWorldInfo result = terrariaWorldFileService.getWorldInfo(worldWithName("Some World"));
        assertNotNull(result);
        assertEquals("Some_World", result.getFileName());
        assertEquals("Some World", result.getDisplayName());
        assertEquals(8L, result.getLastModified().toEpochMilli());

        when(fileService.zip(wldFile, twldFile)).thenReturn("Content".getBytes());
        final TerrariaWorldEntity world = makeWorld();
        final TerrariaWorldFileEntity file = result.readFile(world);
        assertEquals("Content", new String(file.getContent()));
        assertEquals("Some_World.zip", file.getName());
        assertSame(world, file.getWorld());
    }

    @Test
    void testRecreateWorld_noFile() throws IOException {
        final TerrariaWorldEntity world = makeWorld();
        when(terrariaWorldFileRepository.findByWorld(world)).thenReturn(Optional.empty());

        terrariaWorldFileService.recreateWorld(world);
        verify(fileService, never()).unzip(any(), any());

        assertNull(world.getLastModified());
        verify(terrariaWorldRepository, only()).save(world);
    }

    @Test
    void testGetWorldInfo_noWldFile() {
        final File partialWld = mock(File.class);
        when(partialWld.exists()).thenReturn(false);
        when(fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve("NoWld.wld"))).thenReturn(partialWld);
        when(fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve("NoWld.twld"))).thenReturn(mock(File.class));

        assertNull(terrariaWorldFileService.getWorldInfo(worldWithName("NoWld")));
    }

    @Test
    void testGetWorldInfo_noTwldFile() {
        final File partialWld = mock(File.class);
        when(partialWld.exists()).thenReturn(true);
        final File partialTwld = mock(File.class);
        when(partialTwld.exists()).thenReturn(false);
        when(fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve("NoTwld.wld"))).thenReturn(partialWld);
        when(fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve("NoTwld.twld"))).thenReturn(partialTwld);

        assertNull(terrariaWorldFileService.getWorldInfo(worldWithName("NoTwld")));
    }

    @Test
    void testRecreateWorld() throws IOException {
        final TerrariaWorldFileEntity worldFile = mock(TerrariaWorldFileEntity.class);
        when(worldFile.getContent()).thenReturn("Content".getBytes());

        final TerrariaWorldEntity world = makeWorldBuilder().fileName("World")
                .lastModified(Instant.ofEpochMilli(1L))
                .build();
        when(terrariaWorldFileRepository.findByWorld(world)).thenReturn(Optional.of(worldFile));

        terrariaWorldFileService.recreateWorld(world);

        verify(fileService).unzip(inputStreamCaptor.capture(), eq(TERRARIA_WORLD_DIRECTORY.toFile()));
        assertEquals("Content", new String(inputStreamCaptor.getValue().readAllBytes()));

        verify(fileService).setLastModified(TERRARIA_WORLD_DIRECTORY.resolve("World.wld"), Instant.ofEpochMilli(1L));
        verify(fileService).setLastModified(TERRARIA_WORLD_DIRECTORY.resolve("World.twld"), Instant.ofEpochMilli(1L));
    }

    @Test
    void testRecreateWorld_noLastModified() throws IOException {
        final TerrariaWorldFileEntity worldFile = mock(TerrariaWorldFileEntity.class);
        when(worldFile.getContent()).thenReturn("Content".getBytes());

        final TerrariaWorldEntity world = makeWorldBuilder().lastModified(null).build();
        when(terrariaWorldFileRepository.findByWorld(world)).thenReturn(Optional.of(worldFile));

        terrariaWorldFileService.recreateWorld(world);
        verify(fileService, never()).setLastModified(any(), any());
    }

    private void mockWorldWithData(final String fileName, final InputStream dataStream) throws IOException {
        final File[] wldFiles = files(fileName + ".wld");
        when(fileService.listFilesWithExtensions(TERRARIA_WORLD_DIRECTORY.toFile(), "wld")).thenReturn(wldFiles);

        final File wld = existingFile();
        final File twld = existingFile();
        when(fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve(fileName + ".wld"))).thenReturn(wld);
        when(fileService.pathToFile(TERRARIA_WORLD_DIRECTORY.resolve(fileName + ".twld"))).thenReturn(twld);

        when(fileService.readFileInStream(wld)).thenReturn(dataStream);
    }
}