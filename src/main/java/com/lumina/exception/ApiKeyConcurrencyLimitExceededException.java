package com.lumina.exception;

public class ApiKeyConcurrencyLimitExceededException extends RuntimeException {
    public ApiKeyConcurrencyLimitExceededException(String message) {
        super(message);
    }
}
