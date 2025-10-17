package com.hanachain.hanachainbackend.exception;

/**
 * Exception for internal server errors (HTTP 500)
 */
public class InternalServerErrorException extends RuntimeException {

    public InternalServerErrorException(String message) {
        super(message);
    }

    public InternalServerErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
