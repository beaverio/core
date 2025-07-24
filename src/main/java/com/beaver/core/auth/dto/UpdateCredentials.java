package com.beaver.core.auth.dto;

import com.beaver.core.auth.validation.ValidUpdateCredentials;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Builder
@ValidUpdateCredentials
public record UpdateCredentials(
        @NotBlank(message = "`email` is required")
        @Email(message = "Invalid `email` format")
        String email,
        @NotBlank(message = "`newPassword` is required")
        String newPassword,
        @NotBlank(message = "`currentPassword` is required")
        String currentPassword
) {
}
