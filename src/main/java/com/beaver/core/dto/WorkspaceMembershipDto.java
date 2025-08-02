package com.beaver.core.dto;

import lombok.Builder;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@Builder
public record WorkspaceMembershipDto(
        String id,
        WorkspaceDto workspace,
        String roleName,
        Set<String> permissions
) {

    @SuppressWarnings("unchecked")
    public static WorkspaceMembershipDto fromMap(Map<String, Object> membershipMap) {
        Map<String, Object> workspaceMap = (Map<String, Object>) membershipMap.get("workspace");
        List<String> permissionsList = (List<String>) membershipMap.get("permissions");

        return WorkspaceMembershipDto.builder()
                .id(membershipMap.get("id").toString())
                .workspace(WorkspaceDto.fromMap(workspaceMap))
                .roleName((String) membershipMap.get("roleName"))
                .permissions(new HashSet<>(permissionsList))
                .build();
    }

    public Set<String> getAllPermissions() {
        return permissions;
    }
}
