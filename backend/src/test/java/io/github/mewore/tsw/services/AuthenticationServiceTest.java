package io.github.mewore.tsw.services;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.github.mewore.tsw.exceptions.auth.InvalidCredentialsException;
import io.github.mewore.tsw.exceptions.auth.InvalidUsernameException;
import io.github.mewore.tsw.models.AccountEntity;
import io.github.mewore.tsw.models.AccountTypeEntity;
import io.github.mewore.tsw.models.auth.LoginModel;
import io.github.mewore.tsw.models.auth.SessionViewModel;
import io.github.mewore.tsw.models.auth.SignupModel;
import io.github.mewore.tsw.repositories.AccountRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final Charset BINARY_CHARSET = StandardCharsets.US_ASCII;

    private static final String USERNAME = "username";

    private static final String PASSWORD = "password";

    private static final String SESSION = "e0f245dc-e6e4-4f8a-982b-004cbb04e505";

    private static final AccountTypeEntity ACCOUNT_TYPE = AccountTypeEntity.builder().build();

    private static final String NEW_USERNAME = "new-username";

    private static final String INCORRECT_PASSWORD = "incorrect-password";

    @InjectMocks
    private AuthenticationService authenticationService;

    @Mock
    private AccountRepository accountRepository;

    @Spy
    private final PasswordEncoder passwordEncoder = new IdentityPasswordEncoder();

    @Captor
    private ArgumentCaptor<AccountEntity> accountCaptor;

    @Mock
    private Authentication authentication;

    private static AccountEntity.AccountEntityBuilder makeAccount() {
        return AccountEntity.builder()
                .id(1L)
                .username(USERNAME)
                .password(PASSWORD.getBytes(BINARY_CHARSET))
                .session(SESSION.getBytes(BINARY_CHARSET))
                .type(ACCOUNT_TYPE);
    }

    @Test
    void testLogIn_incorrectUsername() {
        when(accountRepository.findByUsername(NEW_USERNAME)).thenReturn(Optional.empty());
        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.logIn(new LoginModel(NEW_USERNAME, PASSWORD)));
    }

    @Test
    void testLogIn() throws InvalidCredentialsException {
        when(accountRepository.findByUsername(USERNAME)).thenReturn(Optional.of(makeAccount().build()));
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final SessionViewModel session = authenticationService.logIn(new LoginModel(USERNAME, PASSWORD));
        verify(accountRepository).save(accountCaptor.capture());
        assertEquals(session.getToken().toString(), new String(accountCaptor.getValue().getSession(), BINARY_CHARSET));
        assertSame(ACCOUNT_TYPE, session.getRole());
    }

    @Test
    void testSignUp() throws InvalidUsernameException {
        when(accountRepository.existsByUsername(NEW_USERNAME)).thenReturn(false);
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final SessionViewModel session = authenticationService.signUp(new SignupModel(NEW_USERNAME, PASSWORD));
        verify(accountRepository).save(accountCaptor.capture());
        assertEquals(session.getToken().toString(), new String(accountCaptor.getValue().getSession(), BINARY_CHARSET));
        assertEquals(PASSWORD, new String(accountCaptor.getValue().getPassword(), BINARY_CHARSET));
        assertSame(null, session.getRole());
    }

    @Test
    void testSignUp_existingUsername() {
        when(accountRepository.existsByUsername(USERNAME)).thenReturn(true);
        assertThrows(InvalidUsernameException.class,
                () -> authenticationService.signUp(new SignupModel(USERNAME, PASSWORD)));
    }

    @Test
    void testLogIn_incorrectPassword() {
        when(accountRepository.findByUsername(USERNAME)).thenReturn(Optional.of(makeAccount().build()));
        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.logIn(new LoginModel(USERNAME, INCORRECT_PASSWORD)));
    }

    @Test
    void testLogOut() throws InvalidCredentialsException {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(USERNAME);
        final AccountEntity accountWithValidSession = makeAccount().sessionExpiration(Instant.MAX).build();
        assertTrue(accountWithValidSession.getSessionExpiration().isAfter(Instant.now()),
                "The session of the user should not have expired yet.");
        when(accountRepository.findByUsername(USERNAME)).thenReturn(Optional.of(accountWithValidSession));

        authenticationService.logOut(authentication);
        verify(accountRepository).save(accountCaptor.capture());
        assertTrue(accountCaptor.getValue().getSessionExpiration().isBefore(Instant.now()),
                "The session of the user should be marked as expired upon logout.");
    }

    @Test
    void testGetAuthenticatedAccountType() throws InvalidCredentialsException {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(USERNAME);
        when(accountRepository.findByUsername(USERNAME)).thenReturn(Optional.of(makeAccount().build()));
        assertSame(ACCOUNT_TYPE, authenticationService.getAuthenticatedAccountType(authentication));
    }

    @Test
    void testGetAuthenticatedAccount_nullAuthentication() {
        assertThrows(InvalidCredentialsException.class, () -> authenticationService.getAuthenticatedAccount(null),
                "Unauthenticated users cannot log out");
    }

    @Test
    void testGetAuthenticatedAccount_unauthenticatedAuthentication() {
        when(authentication.isAuthenticated()).thenReturn(false);
        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.getAuthenticatedAccount(authentication));
    }

    @Test
    void testGetAuthenticatedAccount_nonExistentUser() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(USERNAME);
        when(accountRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.getAuthenticatedAccount(authentication));
    }

    @Test
    void testGetAuthenticatedAccount() throws InvalidCredentialsException {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(USERNAME);
        final AccountEntity account = makeAccount().build();
        when(accountRepository.findByUsername(USERNAME)).thenReturn(Optional.of(account));

        final AccountEntity result = authenticationService.getAuthenticatedAccount(authentication);
        assertSame(account, result);
    }

    @Test
    void testLoadUserByUsername_nonExistentUsername() {
        when(accountRepository.findByUsername(NEW_USERNAME)).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> authenticationService.loadUserByUsername(NEW_USERNAME),
                "Could not find an account with username 'new-username'");
    }

    @Test
    void testLoadUserByUsername() {
        when(accountRepository.findByUsername(USERNAME)).thenReturn(Optional.of(makeAccount().build()));
        final UserDetails result = authenticationService.loadUserByUsername(USERNAME);
        assertEquals(result.getUsername(), USERNAME);
        assertEquals(result.getPassword(), SESSION);
    }

    private static class IdentityPasswordEncoder implements PasswordEncoder {

        @Override
        public String encode(final CharSequence rawPassword) {
            return rawPassword.toString();
        }

        @Override
        public boolean matches(final CharSequence rawPassword, final String encodedPassword) {
            return rawPassword.toString().equals(encodedPassword);
        }
    }
}