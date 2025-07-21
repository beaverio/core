package com.beaver.core.controller;

import com.beaver.core.dto.SigninRequest;
import com.beaver.core.dto.SignupRequest;
import com.beaver.core.dto.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    void shouldRegisterNewUser() {
        String url = "http://localhost:" + port + "/auth/signup";
        SignupRequest request = new SignupRequest("testuser", "test@example.com", "password123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SignupRequest> httpEntity = new HttpEntity<>(request, headers);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(url, httpEntity, AuthResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User registered successfully", response.getBody().getMessage());
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    void shouldRejectDuplicateUsername() {
        String url = "http://localhost:" + port + "/auth/signup";
        
        // First registration
        SignupRequest request1 = new SignupRequest("duplicate", "test1@example.com", "password123");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<SignupRequest> httpEntity1 = new HttpEntity<>(request1, headers);
        restTemplate.postForEntity(url, httpEntity1, AuthResponse.class);

        // Second registration with same username
        SignupRequest request2 = new SignupRequest("duplicate", "test2@example.com", "password123");
        HttpEntity<SignupRequest> httpEntity2 = new HttpEntity<>(request2, headers);
        
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(url, httpEntity2, AuthResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Username already exists", response.getBody().getMessage());
        assertFalse(response.getBody().isSuccess());
    }

    @Test
    void shouldSignInValidUser() {
        String signupUrl = "http://localhost:" + port + "/auth/signup";
        String signinUrl = "http://localhost:" + port + "/auth/signin";
        
        // First register user
        SignupRequest signupRequest = new SignupRequest("signinuser", "signin@example.com", "password123");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<SignupRequest> signupEntity = new HttpEntity<>(signupRequest, headers);
        restTemplate.postForEntity(signupUrl, signupEntity, AuthResponse.class);

        // Then sign in
        SigninRequest signinRequest = new SigninRequest("signinuser", "password123");
        HttpEntity<SigninRequest> signinEntity = new HttpEntity<>(signinRequest, headers);
        
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(signinUrl, signinEntity, AuthResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User signed in successfully", response.getBody().getMessage());
        assertTrue(response.getBody().isSuccess());
        
        // Check for cookies (though TestRestTemplate may not capture them fully)
        assertTrue(response.getHeaders().containsKey("Set-Cookie"));
    }

    @Test
    void shouldRejectInvalidCredentials() {
        String url = "http://localhost:" + port + "/auth/signin";
        SigninRequest request = new SigninRequest("nonexistent", "wrongpassword");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SigninRequest> httpEntity = new HttpEntity<>(request, headers);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(url, httpEntity, AuthResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid credentials", response.getBody().getMessage());
        assertFalse(response.getBody().isSuccess());
    }
}