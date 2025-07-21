package com.beaver.core.controller;

import com.beaver.core.dto.AuthResponse;
import com.beaver.core.dto.SigninRequest;
import com.beaver.core.dto.SignupRequest;
import com.beaver.core.entity.User;
import com.beaver.core.service.UserService;
import com.beaver.core.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request) {
        try {
            User user = userService.registerUser(request.getUsername(), request.getEmail(), request.getPassword());
            return ResponseEntity.ok(new AuthResponse("User registered successfully", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new AuthResponse(e.getMessage(), false));
        }
    }
    
    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> signin(@RequestBody SigninRequest request, HttpServletResponse response) {
        Optional<User> userOpt = userService.authenticateUser(request.getUsername(), request.getPassword());
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String accessToken = jwtUtil.generateAccessToken(user.getUsername());
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());
            
            // Set HTTP-only cookies
            Cookie accessCookie = new Cookie("access_token", accessToken);
            accessCookie.setHttpOnly(true);
            accessCookie.setPath("/");
            accessCookie.setMaxAge(3600); // 1 hour
            
            Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(604800); // 7 days
            
            response.addCookie(accessCookie);
            response.addCookie(refreshCookie);
            
            return ResponseEntity.ok(new AuthResponse("User signed in successfully", true));
        } else {
            return ResponseEntity.badRequest().body(new AuthResponse("Invalid credentials", false));
        }
    }
}