package com.beaver.core.auth.dto;

import com.beaver.core.auth.validation.UniqueEmail;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@UniqueEmail
public record SignupRequest(
        @NotBlank(message = "`{field}` is required")
        @Email(message = "Invalid `{field}` format")
        String email,
        @NotBlank(message = "`{field}` is required")
        String password
) implements IAuthRequest {
}