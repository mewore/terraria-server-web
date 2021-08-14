package io.github.mewore.tsw.repositories.terraria;

import javax.transaction.Transactional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;

@Transactional
public interface TerrariaWorldRepository extends JpaRepository<TerrariaWorldEntity, Long> {

    List<TerrariaWorldEntity> findByHost(final HostEntity host);

    List<TerrariaWorldEntity> findByHostIdOrderByIdAsc(final long hostId);
}
