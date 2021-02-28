package io.github.mewore.tsw.exceptions.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidUsernameException extends Exception {

    public InvalidUsernameException(final String message) {
        super(message);
    }
}
