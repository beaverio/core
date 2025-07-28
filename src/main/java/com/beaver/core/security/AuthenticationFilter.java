package com.beaver.core.security;

import com.beaver.core.config.JwtConfig;
import com.beaver.core.exception.JwtTokenMalformedException;
import com.beaver.core.exception.JwtTokenMissingException;
import com.beaver.core.service.JwtService;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

@RefreshScope
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final JwtService jwtService;
    private final JwtConfig jwtConfig;

    public AuthenticationFilter(JwtService jwtService, JwtConfig jwtConfig) {
        super(Config.class);
        this.jwtService = jwtService;
        this.jwtConfig = jwtConfig;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            if (!jwtConfig.isAuthDisabled()) {
                String token = extractTokenFromCookies(exchange.getRequest());
                
                if (token == null) {
                    log.warn("Access token is missing for request to: {}", exchange.getRequest().getURI().getPath());
                    return Mono.error(new JwtTokenMissingException("Access token is missing"));
                }

                // Reactive JWT validation
                return jwtService.isValidAccessToken(token)
                    .flatMap(isValid -> {
                        if (isValid) {
                            return chain.filter(exchange);
                        } else {
                            log.warn("JWT token validation failed for request to: {}", exchange.getRequest().getURI().getPath());
                            return Mono.error(new JwtTokenMalformedException("Invalid or expired JWT token"));
                        }
                    })
                    .onErrorResume(ex -> {
                        if (ex instanceof JwtTokenMissingException || ex instanceof JwtTokenMalformedException) {
                            return Mono.error(ex);
                        }
                        log.warn("JWT token validation error for request to: {}", exchange.getRequest().getURI().getPath(), ex);
                        return Mono.error(new JwtTokenMalformedException("Invalid or expired JWT token"));
                    });
            }

            return chain.filter(exchange);
        });
    }
    
    private String extractTokenFromCookies(org.springframework.http.server.reactive.ServerHttpRequest request) {
        if (request.getCookies().containsKey("access_token")) {
            return request.getCookies().getFirst("access_token").getValue();
        }
        return null;
    }

    public static class Config {
    }
}