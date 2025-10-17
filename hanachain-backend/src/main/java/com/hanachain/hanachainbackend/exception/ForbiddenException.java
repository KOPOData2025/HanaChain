package com.hanachain.hanachainbackend.exception;

/**
 * Exception for forbidden access (HTTP 403)
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
