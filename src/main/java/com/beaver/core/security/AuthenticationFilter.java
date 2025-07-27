package com.beaver.core.security;

import com.beaver.core.config.JwtConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RefreshScope
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final RouterValidator routerValidator;
    private final JwtTokenUtil jwtTokenUtil;
    private final JwtConfig jwtConfig;
    private final ObjectMapper objectMapper;

    public AuthenticationFilter(RouterValidator routerValidator, JwtTokenUtil jwtTokenUtil, JwtConfig config, ObjectMapper objectMapper) {
        super(Config.class);
        this.routerValidator = routerValidator;
        this.jwtTokenUtil = jwtTokenUtil;
        this.jwtConfig = config;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            if (routerValidator.isSecured.test(exchange.getRequest()) && !jwtConfig.isAuthDisabled()) {
                // Extract JWT token from cookies instead of Authorization header
                String token = extractTokenFromCookies(exchange.getRequest());
                
                if (token == null) {
                    // Return 401 instead of throwing RuntimeException
                    log.warn("Missing JWT token in cookies for request to: {}", exchange.getRequest().getURI().getPath());
                    return handleUnauthorized(exchange.getResponse(), "Missing JWT token in cookies", exchange.getRequest().getURI().toString());
                }

                try {
                    jwtTokenUtil.validateTokenFromCookie(token);
                }
                catch (Exception ex) {
                    log.warn("JWT token validation failed for request to: {}", exchange.getRequest().getURI().getPath());
                    return handleUnauthorized(exchange.getResponse(), "Invalid or expired JWT token", exchange.getRequest().getURI().toString());
                }
            }

            return chain.filter(exchange);
        });
    }
    
    private reactor.core.publisher.Mono<Void> handleUnauthorized(ServerHttpResponse response, String message, String path) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        
        Map<String, Object> errorResponse = Map.of(
            "timestamp", new Date().toString(),
            "status", 401,
            "error", "UNAUTHORIZED",
            "message", message,
            "path", path
        );
        
        try {
            String json = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(json.getBytes());
            return response.writeWith(Flux.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error creating JSON response", e);
            DataBuffer buffer = response.bufferFactory().wrap("{\"error\":\"Unauthorized\"}".getBytes());
            return response.writeWith(Flux.just(buffer));
        }
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