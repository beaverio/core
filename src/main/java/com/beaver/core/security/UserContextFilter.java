package com.beaver.core.security;

import com.beaver.core.service.JwtService;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class UserContextFilter extends AbstractGatewayFilterFactory<UserContextFilter.Config> {

    private final JwtService jwtService;

    public UserContextFilter(JwtService jwtService) {
        super(Config.class);
        this.jwtService = jwtService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String token = extractTokenFromCookies(exchange.getRequest());

            if (token != null) {
                // Extract user ID from token and add as header to downstream request
                return jwtService.extractUserId(token)
                        .flatMap(userId -> {
                            // Add user ID as header to the downstream request
                            var modifiedRequest = exchange.getRequest().mutate()
                                    .header("X-User-Id", userId)
                                    .build();

                            var modifiedExchange = exchange.mutate()
                                    .request(modifiedRequest)
                                    .build();

                            return chain.filter(modifiedExchange);
                        })
                        .onErrorResume(ex -> {
                            // If token parsing fails, continue without adding header
                            return chain.filter(exchange);
                        });
            }

            // No token found, continue without adding header
            return chain.filter(exchange);
        };
    }

    private String extractTokenFromCookies(org.springframework.http.server.reactive.ServerHttpRequest request) {
        if (request.getCookies().containsKey("access_token")) {
            return request.getCookies().getFirst("access_token").getValue();
        }
        return null;
    }

    public static class Config {
        // Configuration properties can be added here if needed
    }
}
