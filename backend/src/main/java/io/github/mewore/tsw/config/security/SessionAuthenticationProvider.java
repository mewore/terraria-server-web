package io.github.mewore.tsw.config.security;

import javax.annotation.PostConstruct;

import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import io.github.mewore.tsw.services.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Component
public class SessionAuthenticationProvider extends DaoAuthenticationProvider {

    private final AuthenticationService authenticationService;

    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    void setUp() {
        setUserDetailsService(authenticationService);
        setPasswordEncoder(passwordEncoder);
    }
}
