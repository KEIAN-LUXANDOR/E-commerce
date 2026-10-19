package com.keianluxandor.backend.common;

// Thrown when something already exists, for example an email that is already registered.
// The error handler turns it into a 409 Conflict response.
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}