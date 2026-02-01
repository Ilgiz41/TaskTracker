package org.example.exceptions;

import lombok.Getter;

public class ValidationException extends RuntimeException {

    @Getter
    String message;

    public ValidationException(String message) {
        super(message, null, false, false);
        this.message = message;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
