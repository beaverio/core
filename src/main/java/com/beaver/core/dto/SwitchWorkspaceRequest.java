package com.beaver.core.dto;

import jakarta.validation.constraints.NotBlank;

public record SwitchWorkspaceRequest(
        @NotBlank(message = "Workspace ID is required")
        String workspaceId
) {}
