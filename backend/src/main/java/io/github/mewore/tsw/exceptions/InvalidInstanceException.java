package io.github.mewore.tsw.exceptions;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidInstanceException extends Exception {

    public InvalidInstanceException(final @Nullable String message) {
        super(message);
    }

    public InvalidInstanceException(final @Nullable String message, final Throwable cause) {
        super(message, cause);
    }
}
