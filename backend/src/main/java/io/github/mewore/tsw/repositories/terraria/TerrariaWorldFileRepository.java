package io.github.mewore.tsw.repositories.terraria;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.mewore.tsw.models.terraria.TerrariaWorldFileEntity;

@Transactional
public interface TerrariaWorldFileRepository extends JpaRepository<TerrariaWorldFileEntity, Long> {

}
