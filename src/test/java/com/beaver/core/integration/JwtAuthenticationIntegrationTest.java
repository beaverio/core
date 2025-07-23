package com.beaver.core.integration;

import com.beaver.core.auth.SigninRequest;
import com.beaver.core.auth.SignupRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JwtAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullAuthenticationFlow_ShouldWork() throws Exception {
        // 1. Test /temp endpoint without authentication
        mockMvc.perform(get("/temp"))
                .andExpect(status().isOk())
                .andExpect(content().string("This is a temporary endpoint for testing purposes."));

        // 2. Register a user
        SignupRequest signupRequest = new SignupRequest("integration@test.com", "password123");
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"));

        // 3. Sign in and get cookies
        SigninRequest signinRequest = new SigninRequest("integration@test.com", "password123");
        MvcResult signinResult = mockMvc.perform(post("/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signinRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().exists("refresh_token"))
                .andReturn();

        // 4. Extract cookies for authenticated requests
        String accessTokenCookie = extractCookieValue(signinResult, "access_token");
        String refreshTokenCookie = extractCookieValue(signinResult, "refresh_token");

        // 5. Test authenticated endpoint (accounts endpoint requires authentication)
        mockMvc.perform(get("/accounts")
                        .cookie(new jakarta.servlet.http.Cookie("access_token", accessTokenCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 6. Test refresh token
        mockMvc.perform(post("/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshTokenCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Tokens refreshed successfully"))
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().exists("refresh_token"));

        // 7. Test logout
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"))
                .andExpect(cookie().maxAge("access_token", 0))
                .andExpect(cookie().maxAge("refresh_token", 0));
    }

    private String extractCookieValue(MvcResult result, String cookieName) {
        String[] cookieHeaders = result.getResponse().getHeaders("Set-Cookie").toArray(new String[0]);
        for (String cookieHeader : cookieHeaders) {
            if (cookieHeader.startsWith(cookieName + "=")) {
                Pattern pattern = Pattern.compile(cookieName + "=([^;]+)");
                Matcher matcher = pattern.matcher(cookieHeader);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        return null;
    }
}