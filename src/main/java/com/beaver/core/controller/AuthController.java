package com.beaver.core.controller;

import com.beaver.core.client.UserServiceClient;
import com.beaver.core.config.JwtConfig;
import com.beaver.core.dto.*;
import com.beaver.core.exception.AuthenticationFailedException;
import com.beaver.core.service.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private final UserServiceClient userServiceClient;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;

    public AuthController(UserServiceClient userServiceClient, JwtService jwtService, JwtConfig jwtConfig) {
        this.userServiceClient = userServiceClient;
        this.jwtService = jwtService;
        this.jwtConfig = jwtConfig;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody LoginRequest request) {
        return userServiceClient.validateCredentials(request.email(), request.password())
                .flatMap(userDto -> {
                    return createAuthResponse(
                            User.builder()
                                .id(userDto.id().toString())
                                .email(userDto.email())
                                .name(userDto.name())
                                .build()
                            , "Login successful");
                });
    }

    @PostMapping("/signup")
    public Mono<ResponseEntity<AuthResponse>> signup(@RequestBody SignupRequest request) {
        return userServiceClient.createUser(request.email(), request.password(), request.name())
                .then(Mono.defer(() -> {
                    return userServiceClient.validateCredentials(request.email(), request.password())
                            .flatMap(userDto -> {
                                return createAuthResponse(
                                        User.builder()
                                            .id(userDto.id().toString())
                                            .email(userDto.email())
                                            .name(userDto.name()).build(),
                                        "Signup successful");
                            });
                }));
    }
    
    @PostMapping("/logout")
    public Mono<ResponseEntity<AuthResponse>> logout() {
        ResponseCookie accessCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .maxAge(0)
                .path("/")
                .build();
        
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false)
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
            return Mono.just(ResponseEntity.status(401).body(AuthResponse.builder().message("Refresh token is missing").build()));
        }
        
        return jwtService.isValidRefreshToken(refreshToken)
                .filter(isValid -> isValid)
                .flatMap(valid -> jwtService.extractUserId(refreshToken)
                        .flatMap(this::generateNewAccessToken))
                .switchIfEmpty(Mono.just(ResponseEntity.status(401).body(AuthResponse.builder().message("Invalid refresh token").build())));
    }

    private Mono<ResponseEntity<AuthResponse>> generateNewAccessToken(String userId) {
        try {
            UUID userUuid = UUID.fromString(userId);
            return userServiceClient.getUserById(userUuid)
                    .flatMap(userDto -> {
                        if (userDto.active()) {
                            String newAccessToken = jwtService.generateAccessToken(
                                    userDto.id().toString(),
                                    userDto.email(),
                                    userDto.name()
                            );

                            ResponseCookie accessCookie = createAccessTokenCookie(newAccessToken);

                            AuthResponse response = AuthResponse.builder()
                                    .message("Token refreshed successfully")
                                    .userId(userDto.id().toString())
                                    .email(userDto.email())
                                    .name(userDto.name())
                                    .build();

                            return Mono.just(ResponseEntity.ok()
                                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                                    .body(response));
                        } else {
                            log.warn("User account is inactive for userId: {}", userDto.id());
                            return Mono.error(new AuthenticationFailedException("User account is inactive"));
                        }
                    });
        } catch (IllegalArgumentException e) {
            return Mono.error(new AuthenticationFailedException("Invalid user ID in token"));
        }
    }

    private Mono<ResponseEntity<AuthResponse>> createAuthResponse(
            User user, String message) {

        String accessToken = jwtService.generateAccessToken(
                user.id(), user.email(), user.name());
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
