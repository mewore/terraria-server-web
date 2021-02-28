package io.github.mewore.tsw.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Component;

@TestConfiguration
public class TestConfig {

    @Bean
    AuthenticationProvider authenticationProvider() {
        return new TestAuthenticationProvider();
    }

    @Component
    private static class TestAuthenticationProvider extends DaoAuthenticationProvider {

        public TestAuthenticationProvider() {
            setUserDetailsService(new InMemoryUserDetailsManager());
        }
    }
}
