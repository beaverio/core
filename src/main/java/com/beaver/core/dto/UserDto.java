package com.beaver.core.dto;

import lombok.Builder;

import java.util.Map;

@Builder
public record UserDto(String id, String email, String name, boolean active) {

    public static UserDto fromMap(Map<String, Object> userMap) {
        return UserDto.builder()
                .id(userMap.get("id").toString())
                .email((String) userMap.get("email"))
                .name((String) userMap.get("name"))
                .active((Boolean) userMap.get("active"))
                .build();
    }
}
