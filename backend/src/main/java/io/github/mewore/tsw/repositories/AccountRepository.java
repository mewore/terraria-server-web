package io.github.mewore.tsw.repositories;

import javax.transaction.Transactional;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.mewore.tsw.models.AccountEntity;

@Transactional
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    Optional<AccountEntity> findByUsername(final String username);

    boolean existsByUsername(final String username);
}
