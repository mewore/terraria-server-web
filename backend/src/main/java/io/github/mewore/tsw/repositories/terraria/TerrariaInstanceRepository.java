package io.github.mewore.tsw.repositories.terraria;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import lombok.NonNull;

@Transactional
public interface TerrariaInstanceRepository extends JpaRepository<TerrariaInstanceEntity, Long> {

    List<@NonNull TerrariaInstanceEntity> findByHostIdOrderByIdAsc(final long hostId);

    Optional<TerrariaInstanceEntity> findOneByHostAndPendingActionNotNull(final HostEntity host);
}
