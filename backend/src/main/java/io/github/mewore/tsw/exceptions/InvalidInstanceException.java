package io.github.mewore.tsw.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidInstanceException extends Exception {

    public InvalidInstanceException(final String message) {
        super(message);
    }

    public InvalidInstanceException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
