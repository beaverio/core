package com.beaver.gateway.filter;

import com.beaver.auth.jwt.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ContextEnrichmentFilter extends AbstractGatewayFilterFactory<ContextEnrichmentFilter.Config> {

    private final JwtService jwtService;
    private final String gatewaySecret;

    public ContextEnrichmentFilter(JwtService jwtService, @Value("${gateway.secret}") String gatewaySecret) {
        super(Config.class);
        this.jwtService = jwtService;
        this.gatewaySecret = gatewaySecret;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();

            var requestBuilder = exchange.getRequest().mutate();
            requestBuilder.header("X-Gateway-Secret", gatewaySecret);

            // Get the validated token from exchange attributes (set by AuthenticationFilter)
            String token = exchange.getAttribute("validated-access-token");
            if (token == null) {
                log.debug("No validated JWT token found for path: {} - adding gateway secret only", path);
                var modifiedRequest = requestBuilder.build();
                var modifiedExchange = exchange.mutate().request(modifiedRequest).build();
                return chain.filter(modifiedExchange);
            }

            return Mono.zip(
                jwtService.extractUserId(token).defaultIfEmpty(""),
                jwtService.extractWorkspaceId(token).defaultIfEmpty(""),
                jwtService.extractRole(token).defaultIfEmpty("")
            ).flatMap(tuple -> {
                String userId = tuple.getT1();
                String workspaceId = tuple.getT2();
                String role = tuple.getT3();

                if (!userId.isEmpty()) {
                    requestBuilder.header("X-User-Id", userId);
                }

                if (!workspaceId.isEmpty()) {
                    requestBuilder.header("X-Workspace-Id", workspaceId);
                }

                if (!role.isEmpty()) {
                    requestBuilder.header("X-User-Role", role);
                }

                var modifiedRequest = requestBuilder.build();
                var modifiedExchange = exchange.mutate().request(modifiedRequest).build();

                log.debug("Added auth headers for user {} in workspace {} with role {} for path: {}",
                         userId, workspaceId, role, path);

                return chain.filter(modifiedExchange);
            }).onErrorResume(ex -> {
                log.warn("Failed to extract claims from JWT for path: {}", path, ex);
                var modifiedRequest = requestBuilder.build();
                var modifiedExchange = exchange.mutate().request(modifiedRequest).build();
                return chain.filter(modifiedExchange);
            });
        };
    }

    public static class Config { }
}
