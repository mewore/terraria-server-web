package io.github.mewore.tsw.repositories.terraria;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventEntity;

@Transactional
public interface TerrariaInstanceEventRepository extends JpaRepository<TerrariaInstanceEventEntity, Long> {

    long deleteByInstance(final TerrariaInstanceEntity instance);
}
