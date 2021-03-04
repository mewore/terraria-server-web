package io.github.mewore.tsw.config.security;

import java.time.Instant;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import io.github.mewore.tsw.models.AccountEntity;
import io.github.mewore.tsw.models.AccountTypeEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountUserDetailsTest {

    private static final AccountEntity ACCOUNT = AccountEntity.builder().username("username").build();

    @Test
    void testGetAuthorities_noRole() {
        final UserDetails user = new AccountUserDetails(ACCOUNT, "");
        assertEquals(Collections.emptySet(), user.getAuthorities());
    }

    @Test
    void testGetAuthorities_emptyRole() {
        final UserDetails user = new AccountUserDetails(ACCOUNT.withType(new AccountTypeEntity()), "");
        assertEquals(Collections.emptySet(), user.getAuthorities());
    }

    @Test
    void testGetAuthorities_manageAccountsRole() {
        final AccountTypeEntity roleWithManageUsers = AccountTypeEntity.builder().allowedToManageAccounts(true).build();
        final UserDetails user = new AccountUserDetails(ACCOUNT.withType(roleWithManageUsers), "");
        assertEquals(Collections.singleton(new SimpleGrantedAuthority("ROLE_MANAGE_ACCOUNTS")), user.getAuthorities());
    }

    @Test
    void testIsAccountNonExpired() {
        assertTrue(new AccountUserDetails(ACCOUNT, "").isAccountNonExpired());
    }

    @Test
    void testIsAccountNonLocked() {
        assertTrue(new AccountUserDetails(ACCOUNT, "").isAccountNonLocked());
    }

    @Test
    void testIsCredentialsNonExpired_notExpired() {
        final UserDetails user = new AccountUserDetails(ACCOUNT.withSessionExpiration(Instant.MAX), "");
        assertTrue(user.isCredentialsNonExpired());
    }

    @Test
    void testIsCredentialsNonExpired_expired() {
        final UserDetails user = new AccountUserDetails(ACCOUNT.withSessionExpiration(Instant.MIN), "");
        assertFalse(user.isCredentialsNonExpired());
    }

    @Test
    void testIsEnabled() {
        assertTrue(new AccountUserDetails(ACCOUNT, "").isEnabled());
    }
}