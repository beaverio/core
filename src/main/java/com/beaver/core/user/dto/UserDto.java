package com.beaver.core.user.dto;

import com.beaver.core.user.User;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record UserDto(
        UUID id,
        String email,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean active
) {
    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .active(user.isActive())
                .build();
    }

    public User toEntity() {
        return User.builder()
                .id(this.id)
                .email(this.email)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .active(this.active)
                .build();
    }
}
