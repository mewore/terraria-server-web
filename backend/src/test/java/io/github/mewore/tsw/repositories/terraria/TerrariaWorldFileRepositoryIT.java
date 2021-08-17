package io.github.mewore.tsw.repositories.terraria;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;
import io.github.mewore.tsw.models.terraria.TerrariaWorldFileEntity;
import io.github.mewore.tsw.repositories.HostRepository;

import static io.github.mewore.tsw.models.HostFactory.makeHost;

@DataJpaTest
class TerrariaWorldFileRepositoryIT {

    private static final String WORLD_NAME = "world";

    @Autowired
    private TerrariaWorldFileRepository terrariaWorldFileRepository;

    @Autowired
    private TerrariaWorldRepository terrariaWorldRepository;

    @Autowired
    private HostRepository hostRepository;

    @Test
    void testSave_bigWorld() {
        final TerrariaWorldFileEntity file = terrariaWorldFileRepository.save(TerrariaWorldFileEntity.builder()
                .name(WORLD_NAME + ".zip")
                .content(new byte[1024])
                .world(makeWorld())
                .build());
        terrariaWorldFileRepository.saveAndFlush(file);
    }

    private TerrariaWorldEntity makeWorld() {
        final HostEntity host = hostRepository.saveAndFlush(makeHost());
        return terrariaWorldRepository.saveAndFlush(TerrariaWorldEntity.builder()
                .fileName(WORLD_NAME.replace(' ', '_'))
                .displayName(WORLD_NAME)
                .lastModified(Instant.now())
                .host(host)
                .build());
    }
}