package io.github.mewore.tsw.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

import io.github.mewore.tsw.config.ConfigConstants;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    private final AuthenticationProvider authenticationProvider;

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        http.authorizeRequests()
                // GET requests are allowed by default and blacklisted one by one
                .antMatchers(HttpMethod.GET, ConfigConstants.API_ROOT + "/**").permitAll()
                // All non-GET requests except login and signup require authentication
                .antMatchers(new String[]{ConfigConstants.API_ROOT +
                        "/**", AuthConfigConstants.AUTH_LOG_OUT_ENDPOINT_URI,
                        AuthConfigConstants.AUTH_PING_ENDPOINT_URI})
                .authenticated()
                .anyRequest()
                .permitAll();

        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        http.csrf().disable();

        http.formLogin().disable();

        http.httpBasic().authenticationEntryPoint(new NoWwwAuthenticateBasicAuthEntryPoint());

        if (h2ConsoleEnabled) {
            http.headers().frameOptions().sameOrigin();
        }
    }

    protected void configure(final AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(authenticationProvider);
    }
}
