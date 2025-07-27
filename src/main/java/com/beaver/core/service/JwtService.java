package com.beaver.core.service;

import com.beaver.core.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    
    private final JwtConfig jwtConfig;
    private final SecretKey secretKey;
    
    public JwtService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.secretKey = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }
    
    public String generateAccessToken(String userId, String email, String name) {
        return generateToken(Map.of(
            "userId", userId,
            "email", email,
            "name", name,
            "type", "access"
        ), jwtConfig.getAccessTokenValidity() * 60 * 1000); // minutes to milliseconds
    }
    
    public String generateRefreshToken(String userId) {
        return generateToken(Map.of(
            "userId", userId,
            "type", "refresh"
        ), jwtConfig.getRefreshTokenValidity() * 60 * 1000); // minutes to milliseconds
    }

    private String generateToken(Map<String, Object> claims, long expirationMs) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(secretKey)
                .compact();
    }
    
    public Mono<String> extractUsername(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }
    
    public Mono<String> extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }
    
    public Mono<String> extractUserName(String token) {
        return extractClaim(token, claims -> claims.get("name", String.class));
    }
    
    public Mono<String> extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }
    
    public Mono<Date> extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    public <T> Mono<T> extractClaim(String token, Function<Claims, T> claimsResolver) {
        return extractAllClaims(token)
                .map(claimsResolver);
    }
    
    public Mono<Claims> extractAllClaims(String token) {
        return Mono.fromCallable(() -> 
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
        ).onErrorMap(Exception.class, ex -> new RuntimeException("Invalid JWT token", ex));
    }
    
    public Mono<Boolean> isTokenExpired(String token) {
        return extractExpiration(token)
                .map(expiration -> expiration.before(new Date()));
    }
    
    public Mono<Boolean> isValidAccessToken(String token) {
        return validateTokenType(token, "access");
    }
    
    public Mono<Boolean> isValidRefreshToken(String token) {
        return validateTokenType(token, "refresh");
    }
    
    private Mono<Boolean> validateTokenType(String token, String expectedType) {
        return extractTokenType(token)
                .filter(expectedType::equals)
                .flatMap(type -> isTokenExpired(token))
                .map(expired -> !expired)
                .defaultIfEmpty(false)
                .onErrorReturn(false);
    }
}