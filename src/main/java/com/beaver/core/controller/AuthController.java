package com.beaver.core.controller;

import com.beaver.core.dto.*;
import com.beaver.core.service.JwtService;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    private final JwtService jwtService;
    
    // Mock user for testing - TODO: Replace with actual user-service calls
    private static final User MOCK_USER = new User(
        "550e8400-e29b-41d4-a716-446655440000", 
        "admin@example.com",
        "Admin User",
        "password"
    );
    
    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }
    
    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody LoginRequest request, ServerWebExchange exchange) {
        return validateCredentials(request.email(), request.password())
                .flatMap(user -> {
                    // Generate tokens
                    String accessToken = jwtService.generateAccessToken(user.id(), user.email(), user.name());
                    String refreshToken = jwtService.generateRefreshToken(user.id());
                    
                    // Set HTTP-only cookies
                    ResponseCookie accessCookie = createTokenCookie("access_token", accessToken, Duration.ofMinutes(15));
                    ResponseCookie refreshCookie = createTokenCookie("refresh_token", refreshToken, Duration.ofHours(24));
                    
                    exchange.getResponse().addCookie(accessCookie);
                    exchange.getResponse().addCookie(refreshCookie);
                    
                    AuthResponse response = new AuthResponse("Login successful", user.id(), user.email(), user.name());
                    return Mono.just(ResponseEntity.ok(response));
                })
                .defaultIfEmpty(ResponseEntity.status(401).body(new AuthResponse("Invalid credentials", null, null, null)));
    }
    
    @PostMapping("/signup")
    public Mono<ResponseEntity<AuthResponse>> signup(@RequestBody SignupRequest request, ServerWebExchange exchange) {
        return createUser(request.email(), request.password(), request.name())
                .flatMap(user -> {
                    // Generate tokens
                    String accessToken = jwtService.generateAccessToken(user.id(), user.email(), user.name());
                    String refreshToken = jwtService.generateRefreshToken(user.id());
                    
                    // Set HTTP-only cookies
                    ResponseCookie accessCookie = createTokenCookie("access_token", accessToken, Duration.ofMinutes(15));
                    ResponseCookie refreshCookie = createTokenCookie("refresh_token", refreshToken, Duration.ofHours(24));
                    
                    exchange.getResponse().addCookie(accessCookie);
                    exchange.getResponse().addCookie(refreshCookie);
                    
                    AuthResponse response = new AuthResponse("Signup successful", user.id(), user.email(), user.name());
                    return Mono.just(ResponseEntity.ok(response));
                })
                .defaultIfEmpty(ResponseEntity.status(400).body(new AuthResponse("User already exists", null, null, null)));
    }
    
    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refresh(ServerWebExchange exchange) {
        return getRefreshTokenFromCookies(exchange)
                .flatMap(refreshToken -> jwtService.isValidRefreshToken(refreshToken)
                        .filter(Boolean::booleanValue)
                        .flatMap(valid -> jwtService.extractUserId(refreshToken))
                        .flatMap(userId -> getUserById(userId))
                        .flatMap(user -> {
                            // Generate new access token
                            String newAccessToken = jwtService.generateAccessToken(user.id(), user.email(), user.name());
                            
                            // Set new access token cookie
                            ResponseCookie accessCookie = createTokenCookie("access_token", newAccessToken, Duration.ofMinutes(15));
                            exchange.getResponse().addCookie(accessCookie);
                            
                            AuthResponse response = new AuthResponse("Token refreshed", user.id(), user.email(), user.name());
                            return Mono.just(ResponseEntity.ok(response));
                        }))
                .defaultIfEmpty(ResponseEntity.status(401).body(new AuthResponse("Invalid refresh token", null, null, null)));
    }
    
    @PostMapping("/logout")
    public Mono<ResponseEntity<AuthResponse>> logout(ServerWebExchange exchange) {
        // Clear cookies by setting them to expire immediately
        ResponseCookie accessCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
                
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
        
        exchange.getResponse().addCookie(accessCookie);
        exchange.getResponse().addCookie(refreshCookie);
        
        AuthResponse response = new AuthResponse("Logout successful", null, null, null);
        return Mono.just(ResponseEntity.ok(response));
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
    
    // Mock get user by ID - TODO: Replace with user-service call
    private Mono<User> getUserById(String userId) {
        if (MOCK_USER.id().equals(userId)) {
            return Mono.just(MOCK_USER);
        }
        return Mono.empty();
    }
    
    private ResponseCookie createTokenCookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true) // Set to false for local development without HTTPS
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
    
    private Mono<String> getRefreshTokenFromCookies(ServerWebExchange exchange) {
        List<HttpCookie> cookies = exchange.getRequest().getCookies().get("refresh_token");
        if (cookies != null && !cookies.isEmpty()) {
            return Mono.just(cookies.get(0).getValue());
        }
        return Mono.empty();
    }
}