package com.beaver.gateway.filter;

import com.beaver.auth.jwt.JwtConfig;
import com.beaver.auth.jwt.JwtService;
import com.beaver.auth.cookie.AuthCookieService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RefreshScope
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final JwtService jwtService;
    private final JwtConfig jwtConfig;
    private final AuthCookieService cookieService;
    private final ObjectMapper objectMapper;

    public AuthenticationFilter(JwtService jwtService, JwtConfig jwtConfig, AuthCookieService cookieService, ObjectMapper objectMapper) {
        super(Config.class);
        this.jwtService = jwtService;
        this.jwtConfig = jwtConfig;
        this.cookieService = cookieService;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();

            if (path.startsWith("/identity/auth/") || path.startsWith("/auth/")) {
                log.debug("Skipping authentication for auth endpoint: {}", path);
                return chain.filter(exchange);
            }

            if (!jwtConfig.isAuthDisabled()) {
                String token = cookieService.extractAccessToken(exchange.getRequest());

                if (token == null) {
                    log.debug("Access token is missing for request to: {}", path);
                    return createUnauthorizedResponse(exchange, "Access token is required");
                }

                return jwtService.isValidAccessToken(token)
                    .flatMap(isValid -> {
                        if (isValid) {
                            // Add the validated token to exchange attributes for downstream filters
                            exchange.getAttributes().put("validated-jwt-token", token);
                            return chain.filter(exchange);
                        } else {
                            log.debug("JWT token validation failed for request to: {}", path);
                            return createUnauthorizedResponse(exchange, "Invalid or expired access token");
                        }
                    })
                    .onErrorResume(ex -> {
                        log.debug("JWT token validation error for request to: {}", path);
                        return createUnauthorizedResponse(exchange, "Invalid or expired access token");
                    });
            }

            return chain.filter(exchange);
        });
    }

    private Mono<Void> createUnauthorizedResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorResponse = Map.of(
            "timestamp", Instant.now().toString(),
            "path", exchange.getRequest().getPath().value(),
            "status", 401,
            "error", "Unauthorized",
            "message", message
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error creating JSON response", e);
            return exchange.getResponse().setComplete();
        }
    }

    public static class Config {
        // Configuration properties can be added here if needed
    }
}