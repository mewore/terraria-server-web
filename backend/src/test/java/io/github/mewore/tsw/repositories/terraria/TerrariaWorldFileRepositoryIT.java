package io.github.mewore.tsw.repositories.terraria;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import io.github.mewore.tsw.models.terraria.TerrariaWorldFileEntity;

@DataJpaTest
class TerrariaWorldFileRepositoryIT {

    private static final String WORLD_NAME = "world";

    @Autowired
    private TerrariaWorldFileRepository terrariaWorldFileRepository;

    @Test
    void testSave_bigWorld() {
        final TerrariaWorldFileEntity data = terrariaWorldFileRepository.save(
                TerrariaWorldFileEntity.builder().name(WORLD_NAME + ".zip").content(new byte[1024]).build());
        terrariaWorldFileRepository.saveAndFlush(data);
    }
}