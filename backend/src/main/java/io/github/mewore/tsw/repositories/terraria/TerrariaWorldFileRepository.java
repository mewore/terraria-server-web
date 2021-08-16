package io.github.mewore.tsw.repositories.terraria;

import javax.transaction.Transactional;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;
import io.github.mewore.tsw.models.terraria.TerrariaWorldFileEntity;

@Transactional
public interface TerrariaWorldFileRepository extends JpaRepository<TerrariaWorldFileEntity, Long> {

    Optional<TerrariaWorldFileEntity> findByWorld(final TerrariaWorldEntity world);
}
