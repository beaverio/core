package com.beaver.core.controller;

import com.beaver.core.config.JwtConfig;
import com.beaver.core.dto.*;
import com.beaver.core.service.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwtService;
    private final JwtConfig jwtConfig;

    // Mock user for testing - TODO: Replace with actual user-service calls
    private static final User MOCK_USER = new User(
        "550e8400-e29b-41d4-a716-446655440000", 
        "admin@example.com",
        "Admin User",
        "password"
    );
    
    public AuthController(JwtService jwtService, JwtConfig jwtConfig) {
        this.jwtService = jwtService;
        this.jwtConfig = jwtConfig;
    }
    
    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody LoginRequest request) {
        return validateCredentials(request.email(), request.password())
                .flatMap(user -> createAuthResponse(user, "Login successful"))
                .defaultIfEmpty(ResponseEntity.status(401).body(new AuthResponse("Invalid credentials", null, null, null)));
    }
    
    @PostMapping("/signup")
    public Mono<ResponseEntity<AuthResponse>> signup(@RequestBody SignupRequest request) {
        return createUser(request.email(), request.password(), request.name())
                .flatMap(user -> createAuthResponse(user, "Signup successful"))
                .defaultIfEmpty(ResponseEntity.status(400).body(new AuthResponse("User already exists", null, null, null)));
    }
    
    @PostMapping("/logout")
    public Mono<ResponseEntity<AuthResponse>> logout() {
        // Create expired cookies to clear both tokens
        ResponseCookie accessCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .maxAge(0) // Expire immediately
                .path("/")
                .build();
        
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .maxAge(0) // Expire immediately
                .path("/")
                .build();
        
        AuthResponse response = new AuthResponse("Logout successful", null, null, null);
        return Mono.just(ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(response));
    }
    
    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refresh(@CookieValue(value = "refresh_token", required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return Mono.just(ResponseEntity.status(401).body(new AuthResponse("Refresh token is missing", null, null, null)));
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
                .switchIfEmpty(Mono.just(ResponseEntity.status(401).body(new AuthResponse("Invalid refresh token", null, null, null))));
    }

    private Mono<ResponseEntity<AuthResponse>> generateNewAccessToken(String userId) {
        // For now, use mock user data - TODO: Replace with actual user service call
        if (MOCK_USER.id().equals(userId)) {
            String newAccessToken = jwtService.generateAccessToken(MOCK_USER.id(), MOCK_USER.email(), MOCK_USER.name());

            ResponseCookie accessCookie = createAccessTokenCookie(newAccessToken);

            AuthResponse response = new AuthResponse("Token refreshed successfully", userId, MOCK_USER.email(), MOCK_USER.name());
            return Mono.just(ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .body(response));
        }

        return Mono.just(ResponseEntity.status(401).body(new AuthResponse("User not found", null, null, null)));
    }
    
    /**
     * Helper method to create authentication response with tokens and cookies
     */
    private Mono<ResponseEntity<AuthResponse>> createAuthResponse(User user, String message) {
        // Generate both access and refresh tokens using JwtService
        String accessToken = jwtService.generateAccessToken(user.id(), user.email(), user.name());
        String refreshToken = jwtService.generateRefreshToken(user.id());

        // Create HTTP-only cookies for security
        ResponseCookie accessCookie = createAccessTokenCookie(accessToken);
        ResponseCookie refreshCookie = createRefreshTokenCookie(refreshToken);

        AuthResponse response = new AuthResponse(message, user.id(), user.email(), user.name());
        return Mono.just(ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(response));
    }

    /**
     * Helper method to create access token cookie
     */
    private ResponseCookie createAccessTokenCookie(String accessToken) {
        return ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(false) // Set to true in production with HTTPS
                .sameSite("Strict")
                .maxAge(jwtConfig.getAccessTokenValidity() * 60) // Convert minutes to seconds
                .path("/")
                .build();
    }

    /**
     * Helper method to create refresh token cookie
     */
    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false) // Set to true in production with HTTPS
                .sameSite("Strict")
                .maxAge(jwtConfig.getRefreshTokenValidity() * 60) // Convert minutes to seconds
                .path("/")
                .build();
    }

    // Mock credential validation - TODO: Replace with user-service call
    private Mono<User> validateCredentials(String email, String password) {
        if (MOCK_USER.email().equals(email) && MOCK_USER.password().equals(password)) {
            return Mono.just(MOCK_USER);
        }
        return Mono.empty();
    }
    
    // Mock user creation - TODO: Replace with user-service call
    private Mono<User> createUser(String email, String password, String name) {
        // For now, only allow creation of the mock user
        if (MOCK_USER.email().equals(email)) {
            return Mono.empty(); // User already exists
        }
        // In real implementation, would call user-service to create user
        User newUser = new User("new-user-id", email, name, password);
        return Mono.just(newUser);
    }
}