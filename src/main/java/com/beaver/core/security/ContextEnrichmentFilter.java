package com.beaver.core.security;

import com.beaver.auth.jwt.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Set;

@Slf4j
@Component
public class ContextEnrichmentFilter extends AbstractGatewayFilterFactory<ContextEnrichmentFilter.Config> {

    private final JwtService jwtService;

    public ContextEnrichmentFilter(JwtService jwtService) {
        super(Config.class);
        this.jwtService = jwtService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();

            // Skip context enrichment for auth endpoints
            if (path.startsWith("/users/auth/")) {
                log.debug("Skipping context enrichment for auth endpoint: {}", path);
                return chain.filter(exchange);
            }

            // Get the validated token from exchange attributes (set by AuthenticationFilter)
            String token = exchange.getAttribute("validated-jwt-token");
            if (token == null) {
                // No token means authentication was skipped or failed
                log.debug("No validated JWT token found for path: {}", path);
                return chain.filter(exchange);
            }

            // Extract claims and add headers for downstream services
            return Mono.zip(
                jwtService.extractUserId(token).defaultIfEmpty(""),
                jwtService.extractWorkspaceId(token).defaultIfEmpty(""),
                jwtService.extractPermissions(token).defaultIfEmpty(Set.of())
            ).flatMap(tuple -> {
                String userId = tuple.getT1();
                String workspaceId = tuple.getT2();
                Set<String> permissions = tuple.getT3();

                // Build the modified request with auth headers
                var requestBuilder = exchange.getRequest().mutate();

                if (!userId.isEmpty()) {
                    requestBuilder.header("X-User-Id", userId);
                }

                if (!workspaceId.isEmpty()) {
                    requestBuilder.header("X-Workspace-Id", workspaceId);
                }

                if (!permissions.isEmpty()) {
                    requestBuilder.header("X-User-Permissions", String.join(",", permissions));
                }

                var modifiedRequest = requestBuilder.build();
                var modifiedExchange = exchange.mutate().request(modifiedRequest).build();

                log.debug("Added auth headers for user {} in workspace {} for path: {}",
                         userId, workspaceId, path);

                return chain.filter(modifiedExchange);
            }).onErrorResume(ex -> {
                log.warn("Failed to extract claims from JWT for path: {}", path, ex);
                // Continue without headers rather than failing the request
                return chain.filter(exchange);
            });
        };
    }

    public static class Config {
        // Configuration properties can be added here if needed
    }
}
