package com.beaver.core.controller;

import com.beaver.core.client.UserServiceClient;
import com.beaver.core.config.JwtConfig;
import com.beaver.core.dto.*;
import com.beaver.core.service.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private final UserServiceClient userServiceClient;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;

    // Mock user for testing - TODO: Replace with actual user-service calls
    private static final User MOCK_USER = new User(
        "550e8400-e29b-41d4-a716-446655440000", 
        "admin@example.com",
        "Admin User",
        "password"
    );
    
    public AuthController(UserServiceClient userServiceClient, JwtService jwtService, JwtConfig jwtConfig) {
        this.userServiceClient = userServiceClient;
        this.jwtService = jwtService;
        this.jwtConfig = jwtConfig;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody LoginRequest request) {
        return userServiceClient.validateCredentials(request.email(), request.password())
                .flatMap(userResponse -> {
                    if (userResponse.isValid()) {
                        return createAuthResponse(
                                User.builder()
                                    .id(userResponse.userId())
                                    .email(userResponse.email())
                                    .name(userResponse.name())
                                        .build()
                                , "Login successful");
                    } else {
                        return Mono.just(ResponseEntity.status(401)
                                .body(AuthResponse.builder().message("Invalid credentials").build()));
                    }
                });
    }

    @PostMapping("/signup")
    public Mono<ResponseEntity<AuthResponse>> signup(@RequestBody SignupRequest request) {
        log.info("Starting signup process for email: {}", request.email());

        return userServiceClient.createUser(request.email(), request.password(), request.name())
                .doOnSuccess(v -> log.info("User created successfully for email: {}", request.email()))
                .doOnError(error -> log.error("User creation failed for email: {}", request.email(), error))
                .then(Mono.defer(() -> {
                    log.info("Proceeding to validate credentials for newly created user: {}", request.email());
                    return userServiceClient.validateCredentials(request.email(), request.password())
                            .doOnNext(userResponse -> {
                                log.info("Credential validation result for {}: isValid={}, userId={}",
                                        request.email(), userResponse.isValid(), userResponse.userId());
                            })
                            .doOnError(error -> log.error("Credential validation failed for email: {}", request.email(), error))
                            .flatMap(userResponse -> {
                                if (userResponse.isValid()) {
                                    log.info("Creating auth response for user: {}", request.email());
                                    return createAuthResponse(
                                            User.builder()
                                                .id(userResponse.userId())
                                                .email(userResponse.email())
                                                .name(userResponse.name()).build(),
                                            "Signup successful");
                                } else {
                                    log.warn("Credential validation returned invalid for email: {} (userId: {}, isActive: {})",
                                            request.email(), userResponse.userId(), userResponse.isActive());
                                    return Mono.just(ResponseEntity.status(401)
                                            .body(AuthResponse.builder().message("Failed to validate newly created user").build()));
                                }
                            });
                }))
                .doOnSuccess(response -> log.info("Signup completed successfully for email: {}, status: {}",
                        request.email(), response != null ? response.getStatusCode() : "null"))
                .doOnError(error -> log.error("Signup process failed for email: {}", request.email(), error))
                .onErrorResume(error -> {
                    log.error("Signup error for email: {}", request.email(), error);
                    // Check if it's a 400 Bad Request (user already exists)
                    if (error.getMessage() != null && error.getMessage().contains("400 Bad Request")) {
                        return Mono.just(ResponseEntity.status(409)
                                .body(AuthResponse.builder().message("User already exists").build()));
                    } else {
                        return Mono.just(ResponseEntity.status(500)
                                .body(AuthResponse.builder().message("Signup failed: " + error.getMessage()).build()));
                    }
                });
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
        
        // Validate refresh token using reactive methods
        return jwtService.isValidRefreshToken(refreshToken)
                .filter(isValid -> isValid)
                .flatMap(valid -> {
                    // Extract user ID from refresh token
                    // For refresh, we need to get user details to generate new access token
                    // Since we only have userId from refresh token, we'll use mock data for now
                    // TODO: Call user service to get full user details
                    return jwtService.extractUserId(refreshToken)
                            .flatMap(this::generateNewAccessToken);
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(401).body(AuthResponse.builder().message("Invalid refresh token").build())));
    }

    private Mono<ResponseEntity<AuthResponse>> generateNewAccessToken(String userId) {
        // For now, use mock user data - TODO: Replace with actual user service call
        if (MOCK_USER.id().equals(userId)) {
            String newAccessToken = jwtService.generateAccessToken(MOCK_USER.id(), MOCK_USER.email(), MOCK_USER.name());

            ResponseCookie accessCookie = createAccessTokenCookie(newAccessToken);

            AuthResponse response = AuthResponse.builder()
                    .message("Token refreshed successfully")
                    .userId(userId)
                    .email(MOCK_USER.email())
                    .name(MOCK_USER.name())
                    .build();

            return Mono.just(ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .body(response));
        }

        return Mono.just(ResponseEntity.status(401).body(AuthResponse.builder().message("User not found").build()));
    }

    private Mono<ResponseEntity<AuthResponse>> createAuthResponse(
            User user, String message) {

        String accessToken = jwtService.generateAccessToken(
                user.id(), user.email(), user.name());
        String refreshToken = jwtService.generateRefreshToken(user.id());

        log.info("Generated tokens for user {}: accessToken length={}, refreshToken length={}",
                user.email(), accessToken != null ? accessToken.length() : 0,
                refreshToken != null ? refreshToken.length() : 0);

        ResponseCookie accessCookie = createAccessTokenCookie(accessToken);
        ResponseCookie refreshCookie = createRefreshTokenCookie(refreshToken);

        log.info("Created cookies: accessCookie={}, refreshCookie={}",
                accessCookie.toString(), refreshCookie.toString());

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
                .secure(false) // Set to true in production with HTTPS
                .sameSite("Strict")
                .maxAge(jwtConfig.getAccessTokenValidity() * 60) // Convert minutes to seconds
                .path("/")
                .build();
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false) // Set to true in production with HTTPS
                .sameSite("Strict")
                .maxAge(jwtConfig.getRefreshTokenValidity() * 60) // Convert minutes to seconds
                .path("/")
                .build();
    }
}
