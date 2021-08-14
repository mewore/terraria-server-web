package io.github.mewore.tsw.services.terraria;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;
import io.github.mewore.tsw.models.terraria.TerrariaWorldFileEntity;
import io.github.mewore.tsw.repositories.HostRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldFileRepository;
import io.github.mewore.tsw.repositories.terraria.TerrariaWorldRepository;
import io.github.mewore.tsw.services.LocalHostService;

import static io.github.mewore.tsw.models.HostFactory.makeHostBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest
class TerrariaWorldServiceIT {

    public static final UUID HOST_UUID = UUID.fromString("a0000000-e6e4-4f8a-982b-004cbb04e505");

    public static final UUID OTHER_HOST_UUID = UUID.fromString("b0000000-e6e4-4f8a-982b-004cbb04e505");

    private static final String WORLD_NAME = "world";

    @Autowired
    private HostRepository hostRepository;

    @MockBean
    private LocalHostService localHostService;

    @MockBean
    private TerrariaWorldFileService terrariaWorldFileService;

    @Autowired
    private TerrariaWorldRepository terrariaWorldRepository;

    @Autowired
    private TerrariaWorldFileRepository terrariaWorldFileRepository;

    private static TerrariaWorldInfo makeWorldInfo(final String name,
            final long lastModified,
            final @Nullable TerrariaWorldFileEntity readResult) throws IOException {
        final TerrariaWorldInfo worldInfo = mock(TerrariaWorldInfo.class);
        when(worldInfo.getName()).thenReturn(name);
        when(worldInfo.getLastModified()).thenReturn(Instant.ofEpochMilli(lastModified));
        if (readResult != null) {
            when(worldInfo.readFile()).thenReturn(readResult);
        }
        return worldInfo;
    }

    private TerrariaWorldService service() {
        return new TerrariaWorldService(localHostService, terrariaWorldFileService, terrariaWorldRepository,
                terrariaWorldFileRepository);
    }

    @Test
    void testSetUp() throws IOException {
        final HostEntity host = saveHost();
        when(localHostService.getOrCreateHost()).thenReturn(host);

        final TerrariaWorldEntity unchangedWorld = terrariaWorldRepository.saveAndFlush(makeWorld(host, "Unchanged"));
        final TerrariaWorldEntity changedWorld = terrariaWorldRepository.saveAndFlush(makeWorld(host, "Changed"));
        terrariaWorldRepository.saveAndFlush(makeWorld(host, "Deleted"));
        final TerrariaWorldEntity otherHostWorld = terrariaWorldRepository.saveAndFlush(makeWorld(saveOtherHost()));

        final TerrariaWorldFileEntity changedWorldFile = TerrariaWorldFileEntity.builder()
                .name("Changed.zip")
                .content("Changed content".getBytes())
                .build();
        final TerrariaWorldFileEntity newWorldFile = TerrariaWorldFileEntity.builder()
                .name("New.zip")
                .content("New content".getBytes())
                .build();
        final List<TerrariaWorldInfo> worldInfoList = List.of(makeWorldInfo("Unchanged", 1L, null),
                makeWorldInfo("Changed", 8L, changedWorldFile), makeWorldInfo("New", 1L, newWorldFile));
        when(terrariaWorldFileService.getAllWorldInfo()).thenReturn(worldInfoList);

        service().setUp();
        final List<TerrariaWorldEntity> worlds = terrariaWorldRepository.findAll();
        assertEquals(4, worlds.size());
        assertSame(unchangedWorld, worlds.get(0));
        assertSame(changedWorld, worlds.get(1));
        assertSame(otherHostWorld, worlds.get(2));

        final TerrariaWorldEntity newWorld = worlds.get(3);
        assertEquals("New", newWorld.getName());
        assertEquals(1L, newWorld.getLastModified().toEpochMilli());

        assertEquals(worlds.size(), terrariaWorldFileRepository.findAll().size(),
                "There should be exactly as many world files as there are worlds");
    }

    private HostEntity saveHost() {
        return hostRepository.saveAndFlush(makeHostBuilder().uuid(HOST_UUID).build());
    }

    private HostEntity saveOtherHost() {
        return hostRepository.saveAndFlush(makeHostBuilder().uuid(OTHER_HOST_UUID).build());
    }

    private TerrariaWorldEntity makeWorld(final HostEntity host) {
        return makeWorld(host, WORLD_NAME);
    }

    private TerrariaWorldEntity makeWorld(final HostEntity host, final String name) {
        final TerrariaWorldFileEntity worldFile = TerrariaWorldFileEntity.builder()
                .name(name + ".zip")
                .content(new byte[0])
                .build();
        return TerrariaWorldEntity.builder()
                .name(name)
                .lastModified(Instant.ofEpochMilli(1L))
                .file(worldFile)
                .host(host)
                .build();
    }
}