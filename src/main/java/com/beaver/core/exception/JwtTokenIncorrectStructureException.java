package com.beaver.core.exception;

public class JwtTokenIncorrectStructureException extends Exception {
    public JwtTokenIncorrectStructureException(String message) {
        super(message);
    }
}