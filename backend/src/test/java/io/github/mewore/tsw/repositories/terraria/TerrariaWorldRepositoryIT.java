package io.github.mewore.tsw.repositories.terraria;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.file.FileDataEntity;
import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;
import io.github.mewore.tsw.repositories.HostRepository;
import io.github.mewore.tsw.repositories.file.FileDataRepository;

import static io.github.mewore.tsw.models.HostFactory.makeHostBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class TerrariaWorldRepositoryIT {

    public static final UUID HOST_UUID = UUID.fromString("a0000000-e6e4-4f8a-982b-004cbb04e505");

    public static final UUID OTHER_HOST_UUID = UUID.fromString("b0000000-e6e4-4f8a-982b-004cbb04e505");

    private static final String WORLD_NAME = "world";

    private static final String WORLD_NAME_2 = "world-2";

    private static final String WORLD_NAME_3 = "world-3";

    @Autowired
    private TerrariaWorldRepository worldRepository;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private FileDataRepository fileDataRepository;

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
    void testFindByHostWithData() {
        final HostEntity host = saveHost();
        final TerrariaWorldEntity world = makeWorld(host, WORLD_NAME);
        final FileDataEntity data = fileDataRepository.save(
                FileDataEntity.builder().name("a").content(new byte[]{1}).build());
        world.setData(data);
        worldRepository.saveAndFlush(world);

        final List<TerrariaWorldEntity> hostWorlds = worldRepository.findByHostWithData(host);
        assertEquals(data.getContent(), hostWorlds.get(0).getData().getContent());
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
        worldRepository.saveAndFlush(makeWorld(host));
        assertThrows(DataIntegrityViolationException.class, () -> worldRepository.saveAndFlush(makeWorld(host)));
    }

    @Test
    void testSave_bigWorld() {
        final TerrariaWorldEntity bigWorld = makeWorld(saveHost());
        final FileDataEntity data = fileDataRepository.save(
                FileDataEntity.builder().name(WORLD_NAME + ".zip").content(new byte[1024]).build());
        bigWorld.setData(data);
        worldRepository.saveAndFlush(bigWorld);
    }

    @Test
    void testSetHostWorlds() {
        final HostEntity host = saveHost();
        final TerrariaWorldEntity firstWorld = worldRepository.saveAndFlush(makeWorld(host));
        final TerrariaWorldEntity secondWorld = worldRepository.saveAndFlush(makeWorld(host, WORLD_NAME_2));
        final TerrariaWorldEntity otherHostWorld = worldRepository.saveAndFlush(makeWorld(saveOtherHost()));
        assertEquals(Arrays.asList(firstWorld, secondWorld, otherHostWorld), worldRepository.findAll());

        final TerrariaWorldEntity newWorld = makeWorld(host, WORLD_NAME_3);
        worldRepository.setHostWorlds(host, Arrays.asList(secondWorld, newWorld));
        assertEquals(Arrays.asList(secondWorld, otherHostWorld, newWorld), worldRepository.findAll());
    }

    @Test
    void testSetHostWorlds_overwrite() {
        final HostEntity host = saveHost();
        final TerrariaWorldEntity initialWorld = makeWorld(host);
        initialWorld.setLastModified(Instant.ofEpochMilli(0L));
        initialWorld.setMods(Set.of("mod"));
        worldRepository.saveAndFlush(initialWorld);

        final TerrariaWorldEntity newWorld = makeWorld(host);
        newWorld.setLastModified(Instant.ofEpochMilli(1L));

        worldRepository.setHostWorlds(host, List.of(newWorld));
        final List<TerrariaWorldEntity> result = worldRepository.findAll();
        assertEquals(initialWorld.getId(), result.get(0).getId());
        assertEquals(1L, result.get(0).getLastModified().toEpochMilli());
        assertNull(result.get(0).getMods());
        assertEquals(newWorld.getData().getId(), result.get(0).getData().getId());
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
        final FileDataEntity data = fileDataRepository.save(
                FileDataEntity.builder().name(name + ".zip").content(new byte[0]).build());
        return TerrariaWorldEntity.builder().name(name).lastModified(Instant.now()).data(data).host(host).build();
    }
}