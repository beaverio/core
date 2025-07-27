package com.beaver.core.dto;

import lombok.Builder;

@Builder
public record AuthResponse(String message, String userId, String email, String name) {
}