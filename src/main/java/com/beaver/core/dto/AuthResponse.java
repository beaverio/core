package com.beaver.core.dto;

public record AuthResponse(String message, String userId, String email, String name) {
}