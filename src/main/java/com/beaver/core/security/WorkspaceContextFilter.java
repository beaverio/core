package com.beaver.core.security;

import com.beaver.core.service.JwtService;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Set;

@Component
public class WorkspaceContextFilter extends AbstractGatewayFilterFactory<WorkspaceContextFilter.Config> {

    private final JwtService jwtService;

    public WorkspaceContextFilter(JwtService jwtService) {
        super(Config.class);
        this.jwtService = jwtService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String token = extractTokenFromCookies(exchange.getRequest());

            if (token != null) {
                return Mono.zip(
                        jwtService.extractUserId(token),
                        jwtService.extractWorkspaceId(token).defaultIfEmpty(""),
                        jwtService.extractPermissions(token).defaultIfEmpty(Set.of())
                ).flatMap(tuple -> {
                    String userId = tuple.getT1();
                    String workspaceId = tuple.getT2();
                    Set<String> permissions = tuple.getT3();

                    var requestBuilder = exchange.getRequest().mutate()
                            .header("X-User-Id", userId);

                    if (!workspaceId.isEmpty()) {
                        requestBuilder.header("X-Workspace-Id", workspaceId);
                    }

                    if (!permissions.isEmpty()) {
                        requestBuilder.header("X-User-Permissions", String.join(",", permissions));
                    }

                    var modifiedRequest = requestBuilder.build();

                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                });
            }

            return chain.filter(exchange);
        };
    }

    private String extractTokenFromCookies(ServerHttpRequest request) {
        if (request.getCookies().containsKey("access_token")) {
            return request.getCookies().getFirst("access_token").getValue();
        }
        return null;
    }

    public static class Config {
        // Configuration properties if needed
    }
}
