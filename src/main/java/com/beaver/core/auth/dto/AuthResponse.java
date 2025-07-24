package com.beaver.core.auth.dto;

import lombok.Builder;

@Builder
public record AuthResponse(String message) { }