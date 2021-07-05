package io.github.mewore.tsw.repositories.terraria;

import javax.transaction.Transactional;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;

@Transactional
public interface TerrariaInstanceRepository extends JpaRepository<TerrariaInstanceEntity, Long> {

    Optional<TerrariaInstanceEntity> findOneByHostAndPendingActionNotNull(final HostEntity host);
}
