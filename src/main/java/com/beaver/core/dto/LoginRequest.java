package com.beaver.core.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record LoginRequest(
        @NotEmpty
        @Email
        String email,
        @NotEmpty
        String password
) { }