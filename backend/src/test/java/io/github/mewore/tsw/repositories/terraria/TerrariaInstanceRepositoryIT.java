package io.github.mewore.tsw.repositories.terraria;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceAction;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory;
import io.github.mewore.tsw.repositories.HostRepository;

import static io.github.mewore.tsw.models.HostFactory.makeHost;
import static io.github.mewore.tsw.models.HostFactory.makeHostBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class TerrariaInstanceRepositoryIT {

    private static final String SMALLEST_UUID = "00000000-0000-0000-0000-000000000000";

    @Autowired
    private TerrariaInstanceRepository terrariaInstanceRepository;

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
        return TerrariaInstanceFactory.makeInstanceBuilder().uuid(uuid).host(host);
    }

    @Test
    void testFindByHostIdOrderByIdAsc() {
        final HostEntity host = hostRepository.saveAndFlush(makeHost());
        final TerrariaInstanceEntity firstInstance = terrariaInstanceRepository.saveAndFlush(
                makeInstance(host, "c").build());
        final TerrariaInstanceEntity secondInstance = terrariaInstanceRepository.saveAndFlush(
                makeInstance(host, "b").build());
        final TerrariaInstanceEntity thirdInstance = terrariaInstanceRepository.saveAndFlush(
                makeInstance(host, "a").build());

        final List<TerrariaInstanceEntity> result = terrariaInstanceRepository.findByHostIdOrderByIdAsc(host.getId());
        assertEquals(Arrays.asList(firstInstance, secondInstance, thirdInstance), result);
    }

    @Test
    void testFindByHostIdOrderByIdAsc_nonExistentHost() {
        final List<TerrariaInstanceEntity> result = terrariaInstanceRepository.findByHostIdOrderByIdAsc(0);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindTopByHostAndPendingActionNotNull() {
        final HostEntity host = hostRepository.saveAndFlush(
                makeHostBuilder().uuid(UUID.fromString(SMALLEST_UUID)).build());
        final TerrariaInstanceEntity instanceWithAction = terrariaInstanceRepository.saveAndFlush(
                makeInstance(host, "a").pendingAction(TerrariaInstanceAction.SET_UP).build());
        terrariaInstanceRepository.saveAndFlush(makeInstance(host, "b").build());

        final HostEntity otherHost = hostRepository.saveAndFlush(makeHostBuilder().uuid(uuidWithSuffix("a")).build());
        terrariaInstanceRepository.saveAndFlush(
                makeInstance(otherHost, "a").pendingAction(TerrariaInstanceAction.SET_UP).build());

        final Optional<TerrariaInstanceEntity> result =
                terrariaInstanceRepository.findTopByHostUuidAndPendingActionNotNull(
                host.getUuid());
        assertTrue(result.isPresent());
        assertSame(instanceWithAction, result.get());
    }

    @Test
    void testSave_sameHostIdAndUuid() {
        final HostEntity host = hostRepository.saveAndFlush(makeHost());
        terrariaInstanceRepository.saveAndFlush(makeInstance(host, "").build());
        assertThrows(DataIntegrityViolationException.class,
                () -> terrariaInstanceRepository.saveAndFlush(makeInstance(host, "").build()));
    }
}