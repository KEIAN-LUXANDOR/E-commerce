package com.keianluxandor.backend.common;

// Wrong login details, or the user is not logged in. Becomes 401.
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}