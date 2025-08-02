package com.beaver.core.dto;

import lombok.Builder;

import java.util.Map;

@Builder
public record WorkspaceDto(String id, String name) {

    public static WorkspaceDto fromMap(Map<String, Object> workspaceMap) {
        return WorkspaceDto.builder()
                .id(workspaceMap.get("id").toString())
                .name((String) workspaceMap.get("name"))
                .build();
    }
}
