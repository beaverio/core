package com.beaver.core.dto;

import lombok.Builder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Builder
public record UserWithWorkspacesDto(UserDto user, List<WorkspaceMembershipDto> workspaces) {

    @SuppressWarnings("unchecked")
    public static UserWithWorkspacesDto fromMap(Map<String, Object> responseMap) {
        Map<String, Object> userMap = (Map<String, Object>) responseMap.get("user");
        List<Map<String, Object>> workspacesList = (List<Map<String, Object>>) responseMap.get("workspaces");

        return UserWithWorkspacesDto.builder()
                .user(UserDto.fromMap(userMap))
                .workspaces(workspacesList.stream()
                        .map(WorkspaceMembershipDto::fromMap)
                        .collect(Collectors.toList()))
                .build();
    }
}