package com.beaver.core.service;

import com.beaver.core.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class JwtServiceTest {

    private JwtService jwtService;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm");
        jwtProperties.setAccessTokenExpiration("15m");
        jwtProperties.setRefreshTokenExpiration("24h");
        
        jwtService = new JwtService(jwtProperties);
    }

    @Test
    void generateAccessToken_ShouldCreateValidToken() {
        String token = jwtService.generateAccessToken("user123", "test@example.com", "Test User");
        
        assert token != null;
        assert !token.isEmpty();
        
        // Verify token claims
        StepVerifier.create(jwtService.extractUserId(token))
                .expectNext("user123")
                .verifyComplete();
                
        StepVerifier.create(jwtService.extractUsername(token))
                .expectNext("test@example.com")
                .verifyComplete();
                
        StepVerifier.create(jwtService.extractUserName(token))
                .expectNext("Test User")
                .verifyComplete();
                
        StepVerifier.create(jwtService.extractTokenType(token))
                .expectNext("access")
                .verifyComplete();
    }

    @Test
    void generateRefreshToken_ShouldCreateValidToken() {
        String token = jwtService.generateRefreshToken("user123");
        
        assert token != null;
        assert !token.isEmpty();
        
        // Verify token claims
        StepVerifier.create(jwtService.extractUserId(token))
                .expectNext("user123")
                .verifyComplete();
                
        StepVerifier.create(jwtService.extractTokenType(token))
                .expectNext("refresh")
                .verifyComplete();
    }

    @Test
    void isValidAccessToken_WithValidToken_ShouldReturnTrue() {
        String token = jwtService.generateAccessToken("user123", "test@example.com", "Test User");
        
        StepVerifier.create(jwtService.isValidAccessToken(token))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void isValidRefreshToken_WithValidToken_ShouldReturnTrue() {
        String token = jwtService.generateRefreshToken("user123");
        
        StepVerifier.create(jwtService.isValidRefreshToken(token))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void isValidAccessToken_WithRefreshToken_ShouldReturnFalse() {
        String refreshToken = jwtService.generateRefreshToken("user123");
        
        StepVerifier.create(jwtService.isValidAccessToken(refreshToken))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void isValidRefreshToken_WithAccessToken_ShouldReturnFalse() {
        String accessToken = jwtService.generateAccessToken("user123", "test@example.com", "Test User");
        
        StepVerifier.create(jwtService.isValidRefreshToken(accessToken))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void extractClaim_WithInvalidToken_ShouldHandleError() {
        String invalidToken = "invalid.jwt.token";
        
        StepVerifier.create(jwtService.extractUserId(invalidToken))
                .expectError(RuntimeException.class)
                .verify();
    }
}