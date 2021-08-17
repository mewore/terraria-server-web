package io.github.mewore.tsw.services.terraria;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

    @Autowired
    private EntityManager entityManager;

    private static TerrariaWorldInfo makeWorldInfo(final String worldName,
            final long lastModified,
            final boolean hasReadResult) throws IOException {
        final TerrariaWorldInfo worldInfo = mock(TerrariaWorldInfo.class);
        when(worldInfo.getFileName()).thenReturn(worldName.replace(' ', '_'));
        when(worldInfo.getDisplayName()).thenReturn(worldName);
        when(worldInfo.getLastModified()).thenReturn(Instant.ofEpochMilli(lastModified));
        if (hasReadResult) {
            when(worldInfo.readFile(any())).thenAnswer(invocation -> TerrariaWorldFileEntity.builder()
                    .name(worldName + ".zip")
                    .content("Content".getBytes())
                    .world(invocation.getArgument(0))
                    .build());
        }
        return worldInfo;
    }

    private TerrariaWorldService service() {
        return new TerrariaWorldService(localHostService, terrariaWorldFileService, terrariaWorldRepository,
                terrariaWorldFileRepository, entityManager);
    }

    @Test
    void testSetUp() throws IOException {
        final HostEntity host = saveHost();
        when(localHostService.getOrCreateHost()).thenReturn(host);

        final TerrariaWorldEntity unchangedWorld = terrariaWorldRepository.saveAndFlush(makeWorld(host, "Unchanged"));
        addFileToWorld(unchangedWorld);
        final TerrariaWorldEntity changedWorld = terrariaWorldRepository.saveAndFlush(makeWorld(host, "Changed"));
        addFileToWorld(changedWorld);
        final TerrariaWorldEntity worldWithoutFile = terrariaWorldRepository.saveAndFlush(
                makeWorld(host, "WithoutFile"));
        final TerrariaWorldEntity deletedWorld = terrariaWorldRepository.saveAndFlush(makeWorld(host, "Deleted"));
        addFileToWorld(deletedWorld);
        final TerrariaWorldEntity otherHostWorld = terrariaWorldRepository.saveAndFlush(makeWorld(saveOtherHost()));

        final List<TerrariaWorldInfo> worldInfoList = List.of(makeWorldInfo("Unchanged", 1L, false),
                makeWorldInfo("Changed", 8L, true), makeWorldInfo("WithoutFile", 8L, true),
                makeWorldInfo("New", 1L, true));
        when(terrariaWorldFileService.getAllWorldInfo()).thenReturn(worldInfoList);

        service().setUp();

        verify(terrariaWorldFileService).recreateWorld(deletedWorld);

        final List<TerrariaWorldEntity> worlds = terrariaWorldRepository.findAll();
        final TerrariaWorldEntity newWorld = worlds.get(worlds.size() - 1);
        assertEquals(List.of(unchangedWorld, changedWorld, worldWithoutFile, deletedWorld, otherHostWorld, newWorld),
                worlds);
        assertEquals("New", newWorld.getDisplayName());
        assertEquals(Instant.ofEpochMilli(1L), newWorld.getLastModified());

        assertEquals(Instant.ofEpochMilli(8L), changedWorld.getLastModified());

        assertEquals(List.of("Unchanged", "Changed", "WithoutFile", "Deleted", "New"),
                terrariaWorldFileRepository.findAll().stream().map(file -> file.getWorld().getDisplayName())
                        .collect(Collectors.toList()));
    }

    @Test
    void testSetUp_massInsert() throws IOException {
        final HostEntity host = saveHost();
        when(localHostService.getOrCreateHost()).thenReturn(host);

        final List<TerrariaWorldInfo> worldInfoList = new ArrayList<>(1000);
        for (int i = 0; i < 1000; i++) {
            worldInfoList.add(makeWorldInfo("New" + i, 1L, true));
        }
        when(terrariaWorldFileService.getAllWorldInfo()).thenReturn(worldInfoList);

        service().setUp();

        assertEquals(1000, terrariaWorldRepository.findAll().size());
        assertEquals(1000, terrariaWorldFileRepository.findAll().size());
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
        return TerrariaWorldEntity.builder()
                .fileName(name.replace(' ', '_'))
                .displayName(name)
                .lastModified(Instant.ofEpochMilli(1L))
                .host(host)
                .build();
    }

    private void addFileToWorld(final TerrariaWorldEntity world) {
        terrariaWorldFileRepository.saveAndFlush(TerrariaWorldFileEntity.builder().name(world.getDisplayName() + ".zip")
                .content(new byte[0])
                .world(world)
                .build());
    }
}