package io.github.mewore.tsw.repositories.terraria;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.file.OperatingSystem;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceAction;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
import io.github.mewore.tsw.repositories.HostRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class TerrariaInstanceRepositoryIT {

    private static final String SMALLEST_UUID = "00000000-0000-0000-0000-000000000000";

    @Autowired
    private TerrariaInstanceRepository instanceRepository;

    @Autowired
    private HostRepository hostRepository;

    private static UUID uuidWithSuffix(final String suffix) {
        return UUID.fromString(SMALLEST_UUID.substring(0, SMALLEST_UUID.length() - suffix.length()) + suffix);
    }

    private static TerrariaInstanceEntity.TerrariaInstanceEntityBuilder makeInstance(final HostEntity host,
            final String uuidSuffix) {
        return makeInstance(host, uuidWithSuffix(uuidSuffix));
    }

    private static TerrariaInstanceEntity.TerrariaInstanceEntityBuilder makeInstance(final HostEntity host,
            final UUID uuid) {
        return TerrariaInstanceEntity.builder()
                .uuid(uuid)
                .location(Path.of("/"))
                .name("instance")
                .terrariaServerUrl("server-url")
                .modLoaderReleaseId(1L)
                .state(TerrariaInstanceState.VALID)
                .host(host);
    }

    @Test
    void testFindByHostIdOrderByIdAsc() {
        final HostEntity host = hostRepository.saveAndFlush(makeHost().build());
        final TerrariaInstanceEntity firstInstance = instanceRepository.saveAndFlush(makeInstance(host, "c").build());
        final TerrariaInstanceEntity secondInstance = instanceRepository.saveAndFlush(makeInstance(host, "b").build());
        final TerrariaInstanceEntity thirdInstance = instanceRepository.saveAndFlush(makeInstance(host, "a").build());

        final List<TerrariaInstanceEntity> result = instanceRepository.findByHostIdOrderByIdAsc(host.getId());
        assertEquals(Arrays.asList(firstInstance, secondInstance, thirdInstance), result);
    }

    @Test
    void testFindByHostIdOrderByIdAsc_nonExistentHost() {
        final List<TerrariaInstanceEntity> result = instanceRepository.findByHostIdOrderByIdAsc(0);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindOneByHostAndPendingActionNotNull() {
        final HostEntity host = hostRepository.saveAndFlush(makeHost().build());
        final TerrariaInstanceEntity instanceWithAction = instanceRepository.saveAndFlush(
                makeInstance(host, "a").pendingAction(TerrariaInstanceAction.SET_UP).build());
        instanceRepository.saveAndFlush(makeInstance(host, "b").build());

        final HostEntity otherHost = hostRepository.saveAndFlush(makeHost().uuid(uuidWithSuffix("a")).build());
        instanceRepository.saveAndFlush(
                makeInstance(otherHost, "a").pendingAction(TerrariaInstanceAction.SET_UP).build());

        final Optional<TerrariaInstanceEntity> result = instanceRepository.findOneByHostAndPendingActionNotNull(host);
        assertTrue(result.isPresent());
        assertSame(instanceWithAction, result.get());
    }

    @Test
    void testSave_sameHostIdAndUuid() {
        final HostEntity host = hostRepository.saveAndFlush(makeHost().build());
        instanceRepository.saveAndFlush(makeInstance(host, "").build());
        assertThrows(DataIntegrityViolationException.class,
                () -> instanceRepository.saveAndFlush(makeInstance(host, "").build()));
    }

    private HostEntity.HostEntityBuilder makeHost() {
        return HostEntity.builder()
                .uuid(UUID.fromString(SMALLEST_UUID))
                .heartbeatDuration(Duration.ZERO)
                .os(OperatingSystem.UNKNOWN);
    }
}