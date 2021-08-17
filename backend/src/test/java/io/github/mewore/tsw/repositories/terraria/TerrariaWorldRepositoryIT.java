package io.github.mewore.tsw.repositories.terraria;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;
import io.github.mewore.tsw.repositories.HostRepository;

import static io.github.mewore.tsw.models.HostFactory.makeHostBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class TerrariaWorldRepositoryIT {

    public static final UUID HOST_UUID = UUID.fromString("a0000000-e6e4-4f8a-982b-004cbb04e505");

    public static final UUID OTHER_HOST_UUID = UUID.fromString("b0000000-e6e4-4f8a-982b-004cbb04e505");

    private static final String WORLD_NAME = "world";

    private static final String WORLD_NAME_2 = "world-2";

    @Autowired
    private TerrariaWorldRepository worldRepository;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private TerrariaWorldFileRepository terrariaWorldFileRepository;

    @Test
    void testFindByHost() {
        final HostEntity host = saveHost();
        final TerrariaWorldEntity firstWorld = worldRepository.saveAndFlush(makeWorld(host, WORLD_NAME));
        final TerrariaWorldEntity secondWorld = worldRepository.saveAndFlush(makeWorld(host, WORLD_NAME_2));
        worldRepository.saveAndFlush(makeWorld(saveOtherHost()));

        final List<TerrariaWorldEntity> hostWorlds = worldRepository.findByHost(host);
        assertEquals(Arrays.asList(firstWorld, secondWorld), hostWorlds);
    }

    @Test
    void testFindByHostIdOrderByIdAsc() {
        final HostEntity host = saveHost();
        final TerrariaWorldEntity firstWorld = worldRepository.saveAndFlush(makeWorld(host, "first"));
        final TerrariaWorldEntity secondWorld = worldRepository.saveAndFlush(makeWorld(host, "second"));
        final TerrariaWorldEntity thirdWorld = worldRepository.saveAndFlush(makeWorld(host, "third"));

        final List<TerrariaWorldEntity> result = worldRepository.findByHostIdOrderByIdAsc(host.getId());
        assertEquals(Arrays.asList(firstWorld, secondWorld, thirdWorld), result);
    }

    @Test
    void testFindByHostIdOrderByIdAsc_nonExistentHost() {
        assertTrue(worldRepository.findByHostIdOrderByIdAsc(0).isEmpty());
    }

    @Test
    void testSave_sameUniqueKey() {
        final HostEntity host = saveHost();
        worldRepository.saveAndFlush(makeWorld(host, "Some_World"));
        assertThrows(DataIntegrityViolationException.class,
                () -> worldRepository.saveAndFlush(makeWorld(host, "Some World")));
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
                .lastModified(Instant.now())
                .host(host)
                .build();
    }
}