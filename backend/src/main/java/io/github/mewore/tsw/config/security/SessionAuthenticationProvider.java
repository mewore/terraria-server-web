package io.github.mewore.tsw.config.security;

import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import io.github.mewore.tsw.services.AuthenticationService;

@Component
public class SessionAuthenticationProvider extends DaoAuthenticationProvider {

    public SessionAuthenticationProvider(final AuthenticationService authenticationService,
            final PasswordEncoder passwordEncoder) {

        setUserDetailsService(authenticationService);
        setPasswordEncoder(passwordEncoder);
    }
}
