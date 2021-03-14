package io.github.mewore.tsw.services;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.config.security.AccountUserDetails;
import io.github.mewore.tsw.exceptions.auth.InvalidCredentialsException;
import io.github.mewore.tsw.exceptions.auth.InvalidUsernameException;
import io.github.mewore.tsw.models.AccountEntity;
import io.github.mewore.tsw.models.AccountTypeEntity;
import io.github.mewore.tsw.models.auth.LoginModel;
import io.github.mewore.tsw.models.auth.SessionViewModel;
import io.github.mewore.tsw.models.auth.SignupModel;
import io.github.mewore.tsw.repositories.AccountRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class AuthenticationService implements UserDetailsService {

    private static final Charset BINARY_CHARSET = StandardCharsets.US_ASCII;

    private static final Duration SESSION_DURATION = Duration.ofHours(1);

    private final AccountRepository accountRepository;

    private final PasswordEncoder encoder;

    public @NonNull SessionViewModel logIn(final @NonNull LoginModel loginModel) throws InvalidCredentialsException {
        final AccountEntity account = accountRepository
                .findByUsername(loginModel.getUsername())
                .orElseThrow(() -> InvalidCredentialsException.forUsername(loginModel.getUsername()));

        if (!encoder.matches(loginModel.getPassword(), new String(account.getPassword(), BINARY_CHARSET))) {
            throw InvalidCredentialsException.forUsername(loginModel.getUsername());
        }

        final UUID token = UUID.randomUUID();
        final byte[] encodedToken = encode(token.toString());
        final AccountEntity savedAccount = accountRepository.save(
                account.withSession(encodedToken).withSessionExpiration(getNewSessionExpiration()));
        return new SessionViewModel(token, savedAccount.getType());
    }

    public @NonNull SessionViewModel signUp(final @NonNull SignupModel signupModel) throws InvalidUsernameException {
        if (accountRepository.existsByUsername(signupModel.getUsername())) {
            throw new InvalidUsernameException(
                    String.format("An account with the username '%s' already exists", signupModel.getUsername()));
        }
        final byte[] encodedPassword = encode(signupModel.getPassword());
        final UUID sessionToken = UUID.randomUUID();
        final byte[] encodedSessionToken = encode(sessionToken.toString());
        final AccountEntity newAccount = new AccountEntity(null, signupModel.getUsername(), encodedPassword,
                encodedSessionToken, getNewSessionExpiration(), null);
        final AccountEntity savedAccount = accountRepository.save(newAccount);
        return new SessionViewModel(sessionToken, savedAccount.getType());
    }

    public void logOut(final @Nullable Authentication authentication) throws InvalidCredentialsException {
        final AccountEntity account = getAuthenticatedAccount(authentication);
        accountRepository.save(account.withSessionExpiration(Instant.now()));
    }

    public AccountTypeEntity getAuthenticatedAccountType(final @Nullable Authentication authentication)
            throws InvalidCredentialsException {
        return getAuthenticatedAccount(authentication).getType();
    }

    public AccountEntity getAuthenticatedAccount(final @Nullable Authentication authentication)
            throws InvalidCredentialsException {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InvalidCredentialsException("Unauthenticated users cannot log out");
        }
        final String username = authentication.getName();
        return accountRepository
                .findByUsername(username)
                .orElseThrow(() -> InvalidCredentialsException.forUsername(username));
    }

    private static Instant getNewSessionExpiration() {
        return Instant.now().plus(SESSION_DURATION);
    }

    private byte[] encode(final String stringToEncode) {
        return encoder.encode(stringToEncode).getBytes(BINARY_CHARSET);
    }

    @NonNull
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        return accountRepository
                .findByUsername(username)
                .map((account) -> new AccountUserDetails(account, new String(account.getSession(), BINARY_CHARSET)))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Could not find an account with username '" + username + "'"));
    }
}
