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

    private static AccountEntity.AccountEntityBuilder makeAccount() {
        return AccountEntity.builder().username("username");
    }

    @Test
    void testGetAuthorities_emptyRole() {
        final UserDetails user =
                new AccountUserDetails(makeAccount().type(AccountTypeEntity.builder().build()).build(), "");
        assertEquals(Collections.emptySet(), user.getAuthorities());
    }

    @Test
    void testGetAuthorities_manageAccountsRole() {
        final AccountTypeEntity typeWithManageUsers = AccountTypeEntity.builder().ableToManageAccounts(true).build();
        final UserDetails user = new AccountUserDetails(makeAccount().type(typeWithManageUsers).build(), "");
        assertEquals(Collections.singleton(new SimpleGrantedAuthority("ROLE_MANAGE_ACCOUNTS")), user.getAuthorities());
    }

    @Test
    void testGetAuthorities_manageHostsRole() {
        final AccountTypeEntity typeWithManageHosts = AccountTypeEntity.builder().ableToManageHosts(true).build();
        final UserDetails user = new AccountUserDetails(makeAccount().type(typeWithManageHosts).build(), "");
        assertEquals(Collections.singleton(new SimpleGrantedAuthority("ROLE_MANAGE_HOSTS")), user.getAuthorities());
    }

    @Test
    void testGetAuthorities_createTerrariaInstances() {
        final AccountTypeEntity typeWithManageHosts = AccountTypeEntity.builder().ableToManageTerraria(true).build();
        final UserDetails user = new AccountUserDetails(makeAccount().type(typeWithManageHosts).build(), "");
        assertEquals(Collections.singleton(new SimpleGrantedAuthority("ROLE_MANAGE_TERRARIA")), user.getAuthorities());
    }

    @Test
    void testIsAccountNonExpired() {
        assertTrue(new AccountUserDetails(makeAccount().build(), "").isAccountNonExpired());
    }

    @Test
    void testIsAccountNonLocked() {
        assertTrue(new AccountUserDetails(makeAccount().build(), "").isAccountNonLocked());
    }

    @Test
    void testIsCredentialsNonExpired_notExpired() {
        final UserDetails user = new AccountUserDetails(makeAccount().sessionExpiration(Instant.MAX).build(), "");
        assertTrue(user.isCredentialsNonExpired());
    }

    @Test
    void testIsEnabled() {
        assertTrue(new AccountUserDetails(ACCOUNT, "").isEnabled());
    }

    @Test
    void testIsCredentialsNonExpired_expired() {
        final UserDetails user = new AccountUserDetails(makeAccount().sessionExpiration(Instant.MIN).build(), "");
        assertFalse(user.isCredentialsNonExpired());
    }
}