package io.github.mewore.tsw.repositories;

import javax.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.mewore.tsw.models.HostEntity;

@Transactional
public interface HostRepository extends JpaRepository<HostEntity, Long> {

    Optional<HostEntity> findByUuid(final UUID uuid);
}
