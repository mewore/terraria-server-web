package io.github.mewore.tsw.config.security;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import io.github.mewore.tsw.models.AccountEntity;
import io.github.mewore.tsw.models.AccountTypeEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AccountUserDetails implements UserDetails {

    private final String username;

    private final String password;

    @Getter(AccessLevel.NONE)
    private final Set<GrantedAuthority> authorities;

    @Getter(AccessLevel.NONE)
    private final Instant expiration;

    public AccountUserDetails(final AccountEntity account, final String password) {
        this(account.getUsername(), password, extractAuthorities(account), account.getSessionExpiration());
    }

    private static Set<GrantedAuthority> extractAuthorities(final AccountEntity account) {
        final AccountTypeEntity role = account.getType();
        if (role == null) {
            return Collections.emptySet();
        }

        final Set<GrantedAuthority> result = new HashSet<>();
        if (role.isAbleToManageAccounts()) {
            result.add(new SimpleGrantedAuthority(AuthorityRoles.MANAGE_ACCOUNTS));
        }
        if (role.isAbleToManageHosts()) {
            result.add(new SimpleGrantedAuthority(AuthorityRoles.MANAGE_HOSTS));
        }
        if (role.isAbleToManageTerraria()) {
            result.add(new SimpleGrantedAuthority(AuthorityRoles.MANAGE_TERRARIA));
        }
        return result;
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return Collections.unmodifiableSet(authorities);
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
