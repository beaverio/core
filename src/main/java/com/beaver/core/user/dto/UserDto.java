package com.beaver.core.user.dto;

import com.beaver.core.common.BaseDto;
import com.beaver.core.user.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class UserDto extends BaseDto {

    private final String email;
    private final boolean isActive;

    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .isActive(user.isActive())
                .build();
    }

    public User toEntity() {
        return User.builder()
                .id(this.getId())
                .email(this.getEmail())
                .createdAt(this.getCreatedAt())
                .updatedAt(this.getUpdatedAt())
                .isActive(this.isActive())
                .build();
    }
}