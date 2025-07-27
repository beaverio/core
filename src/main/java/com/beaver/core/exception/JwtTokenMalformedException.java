package com.beaver.core.exception;

public class JwtTokenMalformedException extends Exception {
    public JwtTokenMalformedException(String message) {
        super(message);
    }
}