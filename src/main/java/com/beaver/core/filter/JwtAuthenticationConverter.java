package com.beaver.core.filter;

import com.beaver.core.service.JwtService;
import org.springframework.http.HttpCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationConverter implements ServerAuthenticationConverter {
    
    private final JwtService jwtService;
    
    public JwtAuthenticationConverter(JwtService jwtService) {
        this.jwtService = jwtService;
    }
    
    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return extractAccessTokenFromCookies(exchange)
                .flatMap(token -> jwtService.isValidAccessToken(token)
                        .filter(Boolean::booleanValue)
                        .flatMap(valid -> createAuthentication(token, exchange)))
                .cast(Authentication.class);
    }
    
    private Mono<String> extractAccessTokenFromCookies(ServerWebExchange exchange) {
        List<HttpCookie> cookies = exchange.getRequest().getCookies().get("access_token");
        if (cookies != null && !cookies.isEmpty()) {
            return Mono.just(cookies.get(0).getValue());
        }
        return Mono.empty();
    }
    
    private Mono<UsernamePasswordAuthenticationToken> createAuthentication(String token, ServerWebExchange exchange) {
        return Mono.zip(
                jwtService.extractUserId(token),
                jwtService.extractUsername(token),
                jwtService.extractUserName(token)
        ).map(tuple -> {
            String userId = tuple.getT1();
            String email = tuple.getT2();
            String name = tuple.getT3();
            
            // Create authentication with user details
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    email, null, authorities);
            
            // Store user details for header injection
            auth.setDetails(new UserDetails(userId, email, name));
            
            return auth;
        });
    }
    
    public record UserDetails(String userId, String email, String name) {}
}