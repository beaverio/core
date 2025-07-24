package com.beaver.core.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@ValidUpdateCredentials
public record UpdateCredentials(
        String email,
        String newPassword,
        String currentPassword
) {
}
