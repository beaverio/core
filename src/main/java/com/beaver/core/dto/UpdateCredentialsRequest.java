package com.beaver.core.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpdateCredentialsRequest(
        @Email(message = "Email must be valid")
        String newEmail,

        @NotBlank(message = "Current password is required")
        String currentPassword,

        @Size(min = 8, message = "New password must be at least 8 characters long")
        String newPassword
) { }
