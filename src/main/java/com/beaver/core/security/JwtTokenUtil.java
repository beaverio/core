package com.beaver.core.security;

import java.util.Date;

import com.beaver.core.config.JwtConfig;
import com.beaver.core.exception.JwtTokenIncorrectStructureException;
import com.beaver.core.exception.JwtTokenMalformedException;
import com.beaver.core.exception.JwtTokenMissingException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtTokenUtil {

    private final JwtConfig config;

    public JwtTokenUtil(JwtConfig config) {
        this.config = config;
    }

    public String generateAccessToken(String id) {
        return generateToken(id, config.getAccessTokenValidity(), "access");
    }

    public String generateRefreshToken(String id) {
        return generateToken(id, config.getRefreshTokenValidity(), "refresh");
    }

    public String generateToken(String id) {
        // Keep backward compatibility - defaults to access token
        return generateAccessToken(id);
    }

    private String generateToken(String id, long validityMinutes, String type) {
        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + validityMinutes * 1000 * 60;
        Date exp = new Date(expMillis);
        
        SecretKey key = Keys.hmacShaKeyFor(config.getSecret().getBytes(StandardCharsets.UTF_8));
        
        return Jwts.builder()
                .subject(id)
                .claim("type", type)
                .issuedAt(new Date(nowMillis))
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public void validateToken(final String header) throws JwtTokenMalformedException, JwtTokenMissingException, JwtTokenIncorrectStructureException {
        try {
            String[] parts = header.split(" ");
            if (parts.length != 2 || !"Bearer".equals(parts[0])) {
                throw new JwtTokenIncorrectStructureException("Incorrect Authentication Structure");
            }

            SecretKey key = Keys.hmacShaKeyFor(config.getSecret().getBytes(StandardCharsets.UTF_8));
            Jwts.parser().verifyWith(key).build().parseSignedClaims(parts[1]);
        } catch (SignatureException ex) {
            throw new JwtTokenMalformedException("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            throw new JwtTokenMalformedException("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            throw new JwtTokenMalformedException("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            throw new JwtTokenMalformedException("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            throw new JwtTokenMissingException("JWT claims string is empty.");
        }
    }

    public void validateTokenFromCookie(final String token) throws JwtTokenMalformedException, JwtTokenMissingException {
        validateTokenFromCookie(token, null); // Accept any token type for access validation
    }

    public void validateTokenFromCookie(final String token, String expectedType) throws JwtTokenMalformedException, JwtTokenMissingException {
        try {
            if (token == null || token.trim().isEmpty()) {
                throw new JwtTokenMissingException("JWT token is missing.");
            }

            SecretKey key = Keys.hmacShaKeyFor(config.getSecret().getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            
            // Validate token type if specified
            if (expectedType != null) {
                String tokenType = claims.get("type", String.class);
                if (!expectedType.equals(tokenType)) {
                    throw new JwtTokenMalformedException("Invalid token type. Expected: " + expectedType + ", but got: " + tokenType);
                }
            }
        } catch (SignatureException ex) {
            throw new JwtTokenMalformedException("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            throw new JwtTokenMalformedException("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            throw new JwtTokenMalformedException("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            throw new JwtTokenMalformedException("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            throw new JwtTokenMissingException("JWT claims string is empty.");
        }
    }

    public String getUserIdFromToken(String token) throws JwtTokenMalformedException {
        try {
            SecretKey key = Keys.hmacShaKeyFor(config.getSecret().getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return claims.getSubject();
        } catch (Exception ex) {
            throw new JwtTokenMalformedException("Cannot extract user ID from token");
        }
    }
}