package com.beaver.core.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UpdateCredentials(
        @NotBlank(message = "`{field}` is required")
        @Email(message = "Invalid `{field}` format")
        String email,
        @NotBlank(message = "`{field}` is required")
        String password,
        @NotBlank(message = "`{field}` is required")
        String newPassword
) implements IAuthRequest {
}
