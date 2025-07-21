package com.beaver.core.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void shouldGenerateValidAccessToken() {
        String username = "testuser";
        String token = jwtUtil.generateAccessToken(username);

        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        assertEquals(username, jwtUtil.getUsernameFromToken(token));
        assertFalse(jwtUtil.isTokenExpired(token));
    }

    @Test
    void shouldGenerateValidRefreshToken() {
        String username = "testuser";
        String token = jwtUtil.generateRefreshToken(username);

        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        assertEquals(username, jwtUtil.getUsernameFromToken(token));
        assertFalse(jwtUtil.isTokenExpired(token));
    }

    @Test
    void shouldRejectInvalidToken() {
        String invalidToken = "invalid.token.here";
        
        assertFalse(jwtUtil.validateToken(invalidToken));
        assertTrue(jwtUtil.isTokenExpired(invalidToken));
    }
}