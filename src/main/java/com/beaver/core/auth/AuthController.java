package com.beaver.core.auth;

//import com.beaver.core.auth.dto.AuthResponse;
//import com.beaver.core.auth.dto.SigninRequest;
//import com.beaver.core.auth.dto.SignupRequest;
//import com.beaver.core.auth.dto.UpdateCredentials;
//import com.beaver.core.user.User;
//import com.beaver.core.user.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
//import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
//import java.util.Optional;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

//    private final UserService userService;
//    private final PasswordEncoder passwordEncoder;
//    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
//    private final CustomUserDetailsService userDetailsService;
//
//    @PostMapping("/signup")
//    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest signupRequest) {
//        try {
//            // Create new user
//            User user = User.builder()
//                    .email(signupRequest.email())
//                    .password(passwordEncoder.encode(signupRequest.password()))
//                    .isActive(true)
//                    .build();
//            userService.saveUser(user);
//
//            return ResponseEntity.ok(AuthResponse.builder()
//                            .message("User registered successfully")
//                            .build()
//            );
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError()
//                    .body(AuthResponse.builder().message("Registration failed: " + e.getMessage()).build());
//        }
//    }
//
//    @PostMapping("/signin")
//    public ResponseEntity<AuthResponse> signin(
//            @Valid @RequestBody SigninRequest signinRequest,
//            HttpServletResponse response)
//    {
//        try {
//            // Check if user exists first
//            Optional<User> user = userService.findByEmail(signinRequest.email());
//            if (user.isEmpty()) {
//                System.out.println("User not found for email: " + signinRequest.email());
//                return ResponseEntity.badRequest()
//                        .body(AuthResponse.builder().message("Invalid credentials").build());
//            }
//
//            // Authenticate user
//            authenticationManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(signinRequest.email(), signinRequest.password())
//            );
//
//            UserDetails userDetails = userDetailsService.loadUserByUsername(signinRequest.email());
//            String accessToken = jwtUtil.generateAccessToken(userDetails);
//            String refreshToken = jwtUtil.generateRefreshToken(userDetails);
//
//            // Set HTTP-only cookies
//            setAuthCookies(response, accessToken, refreshToken);
//
//            return ResponseEntity.ok(AuthResponse.builder().message("Login successful").build());
//        } catch (Exception e) {
//            // Log the actual error for debugging
//            System.err.println("Signin error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
//            e.printStackTrace(); // TODO setup better logging
//            return ResponseEntity.badRequest()
//                    .body(AuthResponse.builder().message("Invalid credentials").build());
//        }
//    }
//
//    @PostMapping("/refresh")
//    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
//        try {
//            String refreshToken = extractRefreshTokenFromCookies(request);
//
//            if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
//                return ResponseEntity.badRequest()
//                        .body(AuthResponse.builder().message("Invalid refresh token").build());
//            }
//
//            String username = jwtUtil.extractUsername(refreshToken);
//            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//
//            String newAccessToken = jwtUtil.generateAccessToken(userDetails);
//            String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);
//
//            setAuthCookies(response, newAccessToken, newRefreshToken);
//
//            return ResponseEntity.ok(AuthResponse.builder().message("Tokens refreshed successfully").build());
//        } catch (Exception e) {
//            return ResponseEntity.badRequest()
//                    .body(AuthResponse.builder().message("Token refresh failed").build());
//        }
//    }
//
//    @PostMapping("/logout")
//    public ResponseEntity<AuthResponse> logout(HttpServletResponse response) {
//        clearAuthCookies(response);
//        return ResponseEntity.ok(AuthResponse.builder().message("Logout successful").build());
//    }
//
//    @PreAuthorize("isAuthenticated()")
//    @PatchMapping("/credentials")
//    public ResponseEntity<AuthResponse> updateCredentials(
//            Authentication authentication,
//            @Valid @RequestBody UpdateCredentials request)
//    {
//        String currentEmail = authentication.getName();
//        if (!currentEmail.equalsIgnoreCase(request.email())) {
//            return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                    .body(AuthResponse.builder().message("Forbidden operation").build());
//        }
//
//        User currentUser = userService.findByEmail(currentEmail)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        if (!passwordEncoder.matches(request.password(), currentUser.getPassword())) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(AuthResponse.builder().message("Invalid current password").build());
//        }
//
//        currentUser.setEmail(request.email());
//        currentUser.setPassword(passwordEncoder.encode(request.newPassword()));
//        userService.saveUser(currentUser);
//
//        return ResponseEntity.ok(AuthResponse.builder().message("Credentials updated successfully").build());
//    }

    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        Cookie accessCookie = new Cookie("access_token", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(jwtUtil.getAccessTokenExpiration());

        Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(jwtUtil.getRefreshTokenExpiration());

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