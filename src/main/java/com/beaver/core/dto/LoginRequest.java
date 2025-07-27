package com.beaver.core.dto;

import lombok.Builder;

@Builder
public record LoginRequest(String email, String password) {
}