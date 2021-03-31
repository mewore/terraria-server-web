package io.github.mewore.tsw.repositories;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;

@Transactional
public interface TerrariaInstanceRepository extends JpaRepository<TerrariaInstanceEntity, Long> {

}
