package com.beaver.core.controller;

import com.beaver.core.dto.*;
import com.beaver.core.security.JwtTokenUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtTokenUtil jwtTokenUtil;
    
    // Mock user for testing - TODO: Replace with actual user-service calls
    private static final User MOCK_USER = new User(
        "550e8400-e29b-41d4-a716-446655440000", 
        "admin@example.com",
        "Admin User",
        "password"
    );
    
    public AuthController(JwtTokenUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
    }
    
    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody LoginRequest request) {
        return validateCredentials(request.email(), request.password())
                .flatMap(user -> {
                    // Generate both access and refresh tokens
                    String accessToken = jwtTokenUtil.generateAccessToken(user.id());
                    String refreshToken = jwtTokenUtil.generateRefreshToken(user.id());
                    
                    // Create HTTP-only cookies for security
                    ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                            .httpOnly(true)
                            .secure(false) // Set to true in production with HTTPS
                            .sameSite("Strict")
                            .maxAge(15 * 60) // 15 minutes
                            .path("/")
                            .build();
                    
                    ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                            .httpOnly(true)
                            .secure(false) // Set to true in production with HTTPS
                            .sameSite("Strict")
                            .maxAge(24 * 60 * 60) // 24 hours
                            .path("/")
                            .build();
                    
                    // Remove token from response body
                    AuthResponse response = new AuthResponse("Login successful", user.id(), user.email(), user.name());
                    return Mono.just(ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                            .body(response));
                })
                .defaultIfEmpty(ResponseEntity.status(401).body(new AuthResponse("Invalid credentials", null, null, null)));
    }
    
    @PostMapping("/signup")
    public Mono<ResponseEntity<AuthResponse>> signup(@RequestBody SignupRequest request) {
        return createUser(request.email(), request.password(), request.name())
                .flatMap(user -> {
                    // Generate both access and refresh tokens
                    String accessToken = jwtTokenUtil.generateAccessToken(user.id());
                    String refreshToken = jwtTokenUtil.generateRefreshToken(user.id());
                    
                    // Create HTTP-only cookies for security
                    ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                            .httpOnly(true)
                            .secure(false) // Set to true in production with HTTPS
                            .sameSite("Strict")
                            .maxAge(15 * 60) // 15 minutes
                            .path("/")
                            .build();
                    
                    ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                            .httpOnly(true)
                            .secure(false) // Set to true in production with HTTPS
                            .sameSite("Strict")
                            .maxAge(24 * 60 * 60) // 24 hours
                            .path("/")
                            .build();
                    
                    // Remove token from response body
                    AuthResponse response = new AuthResponse("Signup successful", user.id(), user.email(), user.name());
                    return Mono.just(ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                            .body(response));
                })
                .defaultIfEmpty(ResponseEntity.status(400).body(new AuthResponse("User already exists", null, null, null)));
    }
    
    @PostMapping("/logout")
    public Mono<ResponseEntity<AuthResponse>> logout() {
        // Create expired cookies to clear both tokens
        ResponseCookie accessCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(false) // Set to true in production with HTTPS
                .sameSite("Strict")
                .maxAge(0) // Expire immediately
                .path("/")
                .build();
        
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false) // Set to true in production with HTTPS
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
        
        try {
            // Validate refresh token with type check
            jwtTokenUtil.validateTokenFromCookie(refreshToken, "refresh");
            
            // Extract user ID from refresh token
            String userId = jwtTokenUtil.getUserIdFromToken(refreshToken);
            
            // Generate new access token
            String newAccessToken = jwtTokenUtil.generateAccessToken(userId);
            
            // Create new access token cookie
            ResponseCookie accessCookie = ResponseCookie.from("access_token", newAccessToken)
                    .httpOnly(true)
                    .secure(false) // Set to true in production with HTTPS
                    .sameSite("Strict")
                    .maxAge(15 * 60) // 15 minutes
                    .path("/")
                    .build();
            
            AuthResponse response = new AuthResponse("Token refreshed successfully", userId, null, null);
            return Mono.just(ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .body(response));
            
        } catch (Exception ex) {
            return Mono.just(ResponseEntity.status(401).body(new AuthResponse("Invalid refresh token", null, null, null)));
        }
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