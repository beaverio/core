package com.beaver.core.controller;

import com.beaver.core.client.UserServiceClient;
import com.beaver.core.config.JwtConfig;
import com.beaver.core.dto.*;
import com.beaver.core.exception.AuthenticationFailedException;
import com.beaver.core.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserServiceClient userServiceClient;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return userServiceClient.validateCredentialsWithWorkspaces(request.email(), request.password())
                .map(UserWithWorkspacesDto::fromMap)
                .flatMap(userWithWorkspaces -> {
                    if (userWithWorkspaces.workspaces().isEmpty()) {
                        return Mono.error(new AuthenticationFailedException("No workspace access"));
                    }

                    // Select primary workspace (first active one, or let user choose)
                    WorkspaceMembershipDto primaryMembership = selectPrimaryWorkspace(userWithWorkspaces.workspaces());

                    return createAuthResponse(userWithWorkspaces.user(), primaryMembership, "Login successful");
                });
    }

    @PostMapping("/signup")
    public Mono<ResponseEntity<AuthResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return userServiceClient.createUser(request.email(), request.password(), request.name())
                .then(Mono.defer(() ->
                        userServiceClient.validateCredentials(request.email(), request.password())
                                .flatMap(userMap ->
                                        createAuthResponse(
                                                User.builder()
                                                        .id(userMap.get("id").toString())
                                                        .email((String) userMap.get("email"))
                                                        .name((String) userMap.get("name")).build(),
                                                null,
                                                "Signup successful"))
                ));
    }

    @PostMapping("/switch-workspace")
    public Mono<ResponseEntity<AuthResponse>> switchWorkspace(
            @CookieValue("access_token") String accessToken,
            @Valid @RequestBody SwitchWorkspaceRequest request) {

        return jwtService.extractUserId(accessToken)
                .flatMap(userId ->
                        userServiceClient.validateWorkspaceAccess(
                                        UUID.fromString(userId),
                                        UUID.fromString(request.workspaceId())
                                )
                                .map(WorkspaceMembershipDto::fromMap)
                                .flatMap(membership ->
                                        userServiceClient.getUserById(UUID.fromString(userId))
                                                .map(UserDto::fromMap)
                                                .flatMap(user -> createAuthResponse(
                                                        user,
                                                        membership,
                                                        "Workspace switched successfully"
                                                ))
                                )
                );
    }
    
    @PostMapping("/logout")
    public Mono<ResponseEntity<AuthResponse>> logout() {
        ResponseCookie accessCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .maxAge(0)
                .path("/")
                .build();
        
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .maxAge(0)
                .path("/")
                .build();
        
        AuthResponse response = AuthResponse.builder().message("Logout successful").build();

        return Mono.just(ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(response));
    }
    
    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refresh(@CookieValue(value = "refresh_token", required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return Mono.error(new AuthenticationFailedException("Refresh token is missing"));
        }
        
        return jwtService.isValidRefreshToken(refreshToken)
                .filter(isValid -> isValid)
                .flatMap(valid -> jwtService.extractUserId(refreshToken)
                        .flatMap(this::generateNewAccessToken))
                .switchIfEmpty(Mono.error(new AuthenticationFailedException("Invalid refresh token")));
    }

    @PatchMapping("/update-credentials")
    public Mono<ResponseEntity<AuthResponse>> updateCredentials(
            @CookieValue(value = "access_token", required = false) String accessToken,
            @Valid @RequestBody UpdateCredentialsRequest request) {

        if (accessToken == null || accessToken.trim().isEmpty()) {
            return Mono.error(new AuthenticationFailedException("Access token is missing"));
        }

        return jwtService.isValidAccessToken(accessToken)
                .filter(isValid -> isValid)
                .flatMap(valid -> jwtService.extractUserId(accessToken))
                .flatMap(userId -> {
                    UUID userUuid = UUID.fromString(userId);

                    // Handle email update
                    if (request.newEmail() != null && !request.newEmail().trim().isEmpty()) {
                        return userServiceClient.updateEmail(userUuid, request.newEmail(), request.currentPassword())
                                .map(UserDto::fromMap)
                                .flatMap(updatedUser -> {
                                    // Email changed, need to generate new tokens with new email
                                    String newAccessToken = jwtService.generateAccessToken(
                                            updatedUser.id(),
                                            updatedUser.email(),
                                            updatedUser.name()
                                    );
                                    String newRefreshToken = jwtService.generateRefreshToken(updatedUser.id());

                                    ResponseCookie accessCookie = createAccessTokenCookie(newAccessToken);
                                    ResponseCookie refreshCookie = createRefreshTokenCookie(newRefreshToken);

                                    AuthResponse response = AuthResponse.builder()
                                            .message("Email updated successfully")
                                            .userId(updatedUser.id())
                                            .email(updatedUser.email())
                                            .name(updatedUser.name())
                                            .build();

                                    return Mono.just(ResponseEntity.ok()
                                            .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                                            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                                            .body(response));
                                });
                    }

                    // Handle password update
                    else if (request.newPassword() != null && !request.newPassword().trim().isEmpty()) {
                        return userServiceClient.updatePassword(userUuid, request.currentPassword(), request.newPassword())
                                .then(Mono.just(ResponseEntity.ok(AuthResponse.builder()
                                        .message("Password updated successfully")
                                        .build())));
                    }
                    else {
                        return Mono.error(new AuthenticationFailedException("No valid update field provided"));
                    }
                })
                .switchIfEmpty(Mono.error(new AuthenticationFailedException("Invalid access token")));
    }

    private Mono<ResponseEntity<AuthResponse>> createAuthResponse(
            User user, WorkspaceMembershipDto membership, String message) {

        if (membership != null) {
            // Multi-workspace token
            String accessToken = jwtService.generateAccessToken(
                    user.id(), user.email(), user.name(),
                    membership.workspace().id(),
                    membership.getAllPermissions()
            );
            String refreshToken = jwtService.generateRefreshToken(user.id());

            ResponseCookie accessCookie = createAccessTokenCookie(accessToken);
            ResponseCookie refreshCookie = createRefreshTokenCookie(refreshToken);

            AuthResponse response = AuthResponse.builder()
                    .message(message)
                    .userId(user.id())
                    .email(user.email())
                    .name(user.name())
                    .build();

            return Mono.just(ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(response));
        } else {
            // Simple token (for signup before workspace assignment)
            String accessToken = jwtService.generateAccessToken(user.id(), user.email(), user.name());
            String refreshToken = jwtService.generateRefreshToken(user.id());

            ResponseCookie accessCookie = createAccessTokenCookie(accessToken);
            ResponseCookie refreshCookie = createRefreshTokenCookie(refreshToken);

            AuthResponse response = AuthResponse.builder()
                    .message(message)
                    .userId(user.id())
                    .email(user.email())
                    .name(user.name())
                    .build();

            return Mono.just(ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(response));
        }
    }

    private Mono<ResponseEntity<AuthResponse>> createAuthResponse(
            UserDto user, WorkspaceMembershipDto membership, String message) {

        String accessToken = jwtService.generateAccessToken(
                user.id(), user.email(), user.name(),
                membership.workspace().id(),
                membership.getAllPermissions()
        );
        String refreshToken = jwtService.generateRefreshToken(user.id());

        ResponseCookie accessCookie = createAccessTokenCookie(accessToken);
        ResponseCookie refreshCookie = createRefreshTokenCookie(refreshToken);

        AuthResponse response = AuthResponse.builder()
                .message(message)
                .userId(user.id())
                .email(user.email())
                .name(user.name())
                .build();

        return Mono.just(ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(response));
    }

    private Mono<ResponseEntity<AuthResponse>> generateNewAccessToken(String userId) {
        try {
            UUID userUuid = UUID.fromString(userId);
            return userServiceClient.getUserById(userUuid)
                    .map(UserDto::fromMap)
                    .flatMap(user -> {
                        if (user.active()) {
                            String newAccessToken = jwtService.generateAccessToken(
                                    user.id(),
                                    user.email(),
                                    user.name()
                            );

                            ResponseCookie accessCookie = createAccessTokenCookie(newAccessToken);

                            AuthResponse response = AuthResponse.builder()
                                    .message("Token refreshed successfully")
                                    .userId(user.id())
                                    .email(user.email())
                                    .name(user.name())
                                    .build();

                            return Mono.just(ResponseEntity.ok()
                                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                                    .body(response));
                        } else {
                            log.warn("User account is inactive for userId: {}", user.id());
                            return Mono.error(new AuthenticationFailedException("User account is inactive"));
                        }
                    });
        } catch (IllegalArgumentException e) {
            return Mono.error(new AuthenticationFailedException("Invalid user ID in token"));
        }
    }

    private WorkspaceMembershipDto selectPrimaryWorkspace(java.util.List<WorkspaceMembershipDto> memberships) {
        // For now, just return the first one
        // Later you could add logic for "last used workspace" or let user choose
        return memberships.get(0);
    }

    private ResponseCookie createAccessTokenCookie(String accessToken) {
        return ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .maxAge(jwtConfig.getAccessTokenValidity() * 60)
                .path("/")
                .build();
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .maxAge(jwtConfig.getRefreshTokenValidity() * 60)
                .path("/")
                .build();
    }
}
