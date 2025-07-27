package com.beaver.core.controller;

import com.beaver.core.dto.*;
import com.beaver.core.security.JwtTokenUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
public class AuthController {

    // Commenting for checkmark
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
                    // Generate JWT token using the user ID
                    String token = jwtTokenUtil.generateToken(user.id());
                    
                    AuthResponse response = new AuthResponse("Login successful", user.id(), user.email(), user.name(), token);
                    return Mono.just(ResponseEntity.ok(response));
                })
                .defaultIfEmpty(ResponseEntity.status(401).body(new AuthResponse("Invalid credentials", null, null, null, null)));
    }
    
    @PostMapping("/signup")
    public Mono<ResponseEntity<AuthResponse>> signup(@RequestBody SignupRequest request) {
        return createUser(request.email(), request.password(), request.name())
                .flatMap(user -> {
                    // Generate JWT token using the user ID
                    String token = jwtTokenUtil.generateToken(user.id());
                    
                    AuthResponse response = new AuthResponse("Signup successful", user.id(), user.email(), user.name(), token);
                    return Mono.just(ResponseEntity.ok(response));
                })
                .defaultIfEmpty(ResponseEntity.status(400).body(new AuthResponse("User already exists", null, null, null, null)));
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