package io.github.mewore.tsw.exceptions.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidCredentialsException extends Exception {

    public InvalidCredentialsException(final String message) {
        super(message);
    }

    public static InvalidCredentialsException forUsername(final String username) {
        return new InvalidCredentialsException(
                String.format("The account '%s' does not exist or the provided password for it is incorrect.",
                        username));
    }
}
