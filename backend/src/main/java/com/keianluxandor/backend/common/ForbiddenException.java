package com.keianluxandor.backend.common;

// Logged in, but not allowed to do this yet. Becomes 403.
// "code" tells the website exactly why, e.g. EMAIL_NOT_VERIFIED.
public class ForbiddenException extends RuntimeException {

    private final String code;

    public ForbiddenException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}