package com.beaver.core.auth.dto;

import com.beaver.core.auth.validation.ValidUpdateCredentials;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
@ValidUpdateCredentials
public record UpdateCredentials(
        @NotBlank(message = "`{field}` is required")
        @Email(message = "Invalid `{field}` format")
        String email,
        @NotBlank(message = "`{field}` is required")
        String newPassword,
        @NotBlank(message = "`{field}` is required")
        String currentPassword
) {
}
