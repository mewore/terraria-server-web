package io.github.mewore.tsw.repositories.terraria;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.file.OperatingSystem;
import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;
import io.github.mewore.tsw.repositories.HostRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class TerrariaWorldRepositoryIT {

    private static final String WORLD_NAME = "world";

    private static final String WORLD_NAME_2 = "world-2";

    private static final String WORLD_NAME_3 = "world-3";

    @Autowired
    private TerrariaWorldRepository worldRepository;

    @Autowired
    private HostRepository hostRepository;

    @Test
    void testFindByHost() {
        final HostEntity host = makeHost();
        final TerrariaWorldEntity firstWorld = worldRepository.saveAndFlush(makeWorld(host, WORLD_NAME));
        final TerrariaWorldEntity secondWorld = worldRepository.saveAndFlush(makeWorld(host, WORLD_NAME_2));
        worldRepository.saveAndFlush(makeWorld(makeHost()));

        final List<TerrariaWorldEntity> hostWorlds = worldRepository.findByHost(host);
        assertEquals(Arrays.asList(firstWorld, secondWorld), hostWorlds);
    }

    @Test
    void testSave_sameUniqueKey() {
        final HostEntity host = makeHost();
        worldRepository.saveAndFlush(makeWorld(host));
        assertThrows(DataIntegrityViolationException.class, () -> worldRepository.saveAndFlush(makeWorld(host)));
    }

    @Test
    void testSave_bigWorld() {
        worldRepository.saveAndFlush(makeWorld(makeHost()).withData(new byte[1024]));
    }

    @Test
    void testSetHostWorlds() {
        final HostEntity host = makeHost();
        final TerrariaWorldEntity firstWorld = worldRepository.saveAndFlush(makeWorld(host));
        final TerrariaWorldEntity secondWorld = worldRepository.saveAndFlush(makeWorld(host, WORLD_NAME_2));
        final TerrariaWorldEntity otherHostWorld = worldRepository.saveAndFlush(makeWorld(makeHost()));
        assertEquals(Arrays.asList(firstWorld, secondWorld, otherHostWorld), worldRepository.findAll());

        final TerrariaWorldEntity newWorld = makeWorld(host, WORLD_NAME_3);
        worldRepository.setHostWorlds(host, Arrays.asList(secondWorld, newWorld));
        assertEquals(Arrays.asList(secondWorld, otherHostWorld, newWorld), worldRepository.findAll());
    }

    private static TerrariaWorldEntity makeWorld(final HostEntity host) {
        return makeWorld(host, WORLD_NAME);
    }

    private static TerrariaWorldEntity makeWorld(final HostEntity host, final String name) {
        return TerrariaWorldEntity.builder()
                .name(name)
                .lastModified(Instant.now())
                .data(new byte[0])
                .host(host)
                .build();
    }

    private HostEntity makeHost() {
        return hostRepository.saveAndFlush(HostEntity.builder()
                .uuid(UUID.randomUUID())
                .os(OperatingSystem.LINUX)
                .heartbeatDuration(Duration.ZERO)
                .build());
    }
}