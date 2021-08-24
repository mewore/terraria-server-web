package io.github.mewore.tsw.repositories.terraria;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.world.TerrariaWorldEntity;

@Transactional
public interface TerrariaInstanceRepository extends JpaRepository<TerrariaInstanceEntity, Long> {

    List<TerrariaInstanceEntity> findByHostIdOrderByIdAsc(final long hostId);

    List<TerrariaInstanceEntity> findByHostUuid(final UUID hostUuid);

    Optional<TerrariaInstanceEntity> findTopByHostUuidAndPendingActionNotNull(final UUID hostUuid);

    boolean existsByWorld(final TerrariaWorldEntity world);
}
