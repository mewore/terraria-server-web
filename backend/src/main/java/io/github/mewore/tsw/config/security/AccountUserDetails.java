package io.github.mewore.tsw.config.security;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import io.github.mewore.tsw.models.AccountEntity;
import io.github.mewore.tsw.models.AccountRoleEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AccountUserDetails implements UserDetails {

    private static final GrantedAuthority MANAGE_USERS_AUTHORITY = new SimpleGrantedAuthority("manageUsers");

    private final String username;

    private final String password;

    private final Collection<GrantedAuthority> authorities;

    private final Instant expiration;

    public AccountUserDetails(final AccountEntity account, final String password) {
        this(account.getUsername(), password, extractAuthorities(account), account.getSessionExpiration());
    }

    private static Collection<GrantedAuthority> extractAuthorities(final AccountEntity account) {
        final AccountRoleEntity role = account.getRole();
        if (role == null) {
            return Collections.emptySet();
        }

        return Stream.of(role.isManageAccounts() ? MANAGE_USERS_AUTHORITY : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return Instant.now().isBefore(expiration);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
