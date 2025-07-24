package com.beaver.core.auth;

import com.beaver.core.auth.dto.AuthResponse;
import com.beaver.core.auth.dto.SigninRequest;
import com.beaver.core.auth.dto.SignupRequest;
import com.beaver.core.auth.dto.UpdateCredentials;
import com.beaver.core.user.User;
import com.beaver.core.user.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public AuthController(UserService userService, PasswordEncoder passwordEncoder,
                         AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                         CustomUserDetailsService userDetailsService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest signupRequest) {
        try {
            // Check if user already exists
            if (userService.findByEmail(signupRequest.email()).isPresent()) {
                return ResponseEntity.badRequest()
                        .body(new AuthResponse("User with this email already exists"));
            }

            // Create new user
            User user = User.builder()
                    .email(signupRequest.email())
                    .password(passwordEncoder.encode(signupRequest.password()))
                    .active(true)
                    .build();
            userService.createUser(user);

            return ResponseEntity.ok(new AuthResponse("User registered successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new AuthResponse("Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> signin(@RequestBody SigninRequest signinRequest, 
                                             HttpServletResponse response) {
        try {
            System.out.println("Attempting signin for email: " + signinRequest.email());

            // Check if user exists first
            Optional<User> user = userService.findByEmail(signinRequest.email());
            if (user.isEmpty()) {
                System.out.println("User not found for email: " + signinRequest.email());
                return ResponseEntity.badRequest()
                        .body(new AuthResponse("Invalid credentials"));
            }

            System.out.println("User found, attempting authentication...");

            // Authenticate user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(signinRequest.email(), signinRequest.password())
            );

            System.out.println("Authentication successful, generating tokens...");

            UserDetails userDetails = userDetailsService.loadUserByUsername(signinRequest.email());
            String accessToken = jwtUtil.generateAccessToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            // Set HTTP-only cookies
            setAuthCookies(response, accessToken, refreshToken);

            return ResponseEntity.ok(new AuthResponse("Login successful"));
        } catch (Exception e) {
            // Log the actual error for debugging
            System.err.println("Signin error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new AuthResponse("Invalid credentials"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = extractRefreshTokenFromCookies(request);
            
            if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
                return ResponseEntity.badRequest()
                        .body(new AuthResponse("Invalid refresh token"));
            }

            String username = jwtUtil.extractUsername(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            String newAccessToken = jwtUtil.generateAccessToken(userDetails);
            String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);

            setAuthCookies(response, newAccessToken, newRefreshToken);

            return ResponseEntity.ok(new AuthResponse("Tokens refreshed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse("Token refresh failed"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(HttpServletResponse response) {
        // Clear cookies
        clearAuthCookies(response);
        return ResponseEntity.ok(new AuthResponse("Logout successful"));
    }

    @PatchMapping("/credentials")
    public ResponseEntity<AuthResponse> updateCredentials(Authentication authentication,
                                                         @Valid @RequestBody UpdateCredentials request) {

        System.out.println("Updating credentials: " + request.toString());

        return ResponseEntity.ok(new AuthResponse("Credentials updated successfully"));
    }

    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        Cookie accessCookie = new Cookie("access_token", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(15 * 60);

        Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true); // Set to true in production with HTTPS
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60);

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
    }

    private void clearAuthCookies(HttpServletResponse response) {
        Cookie accessCookie = new Cookie("access_token", "");
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);

        Cookie refreshCookie = new Cookie("refresh_token", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
    }

    private String extractRefreshTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> "refresh_token".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}