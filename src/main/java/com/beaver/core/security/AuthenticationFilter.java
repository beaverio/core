package com.beaver.core.security;

import com.beaver.auth.jwt.JwtConfig;
import com.beaver.auth.exceptions.JwtTokenMalformedException;
import com.beaver.auth.exceptions.JwtTokenMissingException;
import com.beaver.auth.jwt.JwtService;
import com.beaver.auth.cookie.ReactiveTokenExtractor;
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
    private final ReactiveTokenExtractor tokenExtractor;

    public AuthenticationFilter(JwtService jwtService, JwtConfig jwtConfig, ReactiveTokenExtractor tokenExtractor) {
        super(Config.class);
        this.jwtService = jwtService;
        this.jwtConfig = jwtConfig;
        this.tokenExtractor = tokenExtractor;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();

            if (path.startsWith("/users/auth/") || path.startsWith("/auth/")) {
                log.debug("Skipping authentication for auth endpoint: {}", path);
                return chain.filter(exchange);
            }

            if (!jwtConfig.isAuthDisabled()) {
                String token = tokenExtractor.extractAccessToken(exchange.getRequest());

                if (token == null) {
                    log.warn("Access token is missing for request to: {}", path);
                    return Mono.error(new JwtTokenMissingException("Access token is missing"));
                }

                return jwtService.isValidAccessToken(token)
                    .flatMap(isValid -> {
                        if (isValid) {
                            // Add the validated token to exchange attributes for downstream filters
                            exchange.getAttributes().put("validated-jwt-token", token);
                            return chain.filter(exchange);
                        } else {
                            log.warn("JWT token validation failed for request to: {}", path);
                            return Mono.error(new JwtTokenMalformedException("Invalid or expired JWT token"));
                        }
                    })
                    .onErrorResume(ex -> {
                        if (ex instanceof JwtTokenMissingException || ex instanceof JwtTokenMalformedException) {
                            return Mono.error(ex);
                        }
                        log.warn("JWT token validation error for request to: {}", path, ex);
                        return Mono.error(new JwtTokenMalformedException("Invalid or expired JWT token"));
                    });
            }

            return chain.filter(exchange);
        });
    }


    public static class Config {
        // Configuration properties can be added here if needed
    }
}