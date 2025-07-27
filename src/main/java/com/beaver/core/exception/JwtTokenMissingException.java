package com.beaver.core.exception;

public class JwtTokenMissingException extends Exception {
    public JwtTokenMissingException(String message) {
        super(message);
    }
}