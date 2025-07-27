package com.beaver.core.controller;

import com.beaver.core.dto.AuthResponse;
import com.beaver.core.dto.LoginRequest;
import com.beaver.core.dto.SignupRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.http.MediaType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void login_WithValidCredentials_ShouldReturnSuccessAndSetCookies() {
        LoginRequest loginRequest = new LoginRequest("admin@example.com", "password");

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("Set-Cookie")
                .expectBody(AuthResponse.class)
                .value(response -> {
                    assert response.message().equals("Login successful");
                    assert response.email().equals("admin@example.com");
                    assert response.userId().equals("550e8400-e29b-41d4-a716-446655440000");
                });
    }

    @Test
    void login_WithInvalidCredentials_ShouldReturnUnauthorized() {
        LoginRequest loginRequest = new LoginRequest("wrong@example.com", "wrongpassword");

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(AuthResponse.class)
                .value(response -> {
                    assert response.message().equals("Invalid credentials");
                    assert response.email() == null;
                });
    }

    @Test
    void signup_WithNewUser_ShouldReturnSuccessAndSetCookies() {
        SignupRequest signupRequest = new SignupRequest("newuser@example.com", "password", "New User");

        webTestClient.post()
                .uri("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(signupRequest)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("Set-Cookie")
                .expectBody(AuthResponse.class)
                .value(response -> {
                    assert response.message().equals("Signup successful");
                    assert response.email().equals("newuser@example.com");
                    assert response.name().equals("New User");
                });
    }

    @Test
    void logout_ShouldClearCookies() {
        webTestClient.post()
                .uri("/auth/logout")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("Set-Cookie")
                .expectBody(AuthResponse.class)
                .value(response -> {
                    assert response.message().equals("Logout successful");
                });
    }

    @Test
    void healthEndpoint_ShouldBeAccessibleWithoutAuthentication() {
        webTestClient.get()
                .uri("/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Gateway is running!");
    }
}