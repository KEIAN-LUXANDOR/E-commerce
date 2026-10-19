package com.keianluxandor.backend.common;

// Too many failed attempts, e.g. wrong passwords. Becomes 429.
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}