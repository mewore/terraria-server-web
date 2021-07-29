package io.github.mewore.tsw.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidRequestException extends Exception {

    public InvalidRequestException(final String message) {
        super(message);
    }
}
