package io.github.mewore.tsw.repositories;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import io.github.mewore.tsw.models.AccountEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class AccountRepositoryIT {

    private static final String USERNAME = "username";

    @Autowired
    private AccountRepository accountRepository;

    private static AccountEntity.AccountEntityBuilder makeAccount() {
        return AccountEntity.builder().username(USERNAME).password(new byte[0]).session(new byte[0]);
    }

    @Test
    void testFindByUsername() {
        accountRepository.save(makeAccount().build());
        final Optional<AccountEntity> result = accountRepository.findByUsername(USERNAME);
        assertTrue(result.isPresent(), "The account should exist once saved.");
        assertEquals(USERNAME, result.get().getUsername());
    }

    @Test
    void testExistsByUsername() {
        assertFalse(accountRepository.existsByUsername(USERNAME));
        accountRepository.save(makeAccount().build());
        assertTrue(accountRepository.existsByUsername(USERNAME));
    }
}