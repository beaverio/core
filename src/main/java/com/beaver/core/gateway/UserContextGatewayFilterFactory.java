package com.beaver.core.gateway;

import com.beaver.core.filter.JwtAuthenticationConverter;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class UserContextGatewayFilterFactory extends AbstractGatewayFilterFactory<UserContextGatewayFilterFactory.Config> {

    public UserContextGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            return ReactiveSecurityContextHolder.getContext()
                    .map(securityContext -> securityContext.getAuthentication())
                    .cast(Authentication.class)
                    .flatMap(authentication -> {
                        if (authentication != null && authentication.isAuthenticated() 
                            && authentication.getDetails() instanceof JwtAuthenticationConverter.UserDetails userDetails) {
                            
                            // Add user context headers to the request
                            return chain.filter(exchange.mutate()
                                    .request(r -> r.headers(headers -> {
                                        headers.set("X-User-Id", userDetails.userId());
                                        headers.set("X-User-Email", userDetails.email());
                                        headers.set("X-User-Name", userDetails.name());
                                        headers.set("X-User-Roles", "ROLE_USER");
                                    }))
                                    .build());
                        }
                        
                        // No authentication context, proceed without adding headers
                        return chain.filter(exchange);
                    })
                    .switchIfEmpty(chain.filter(exchange));
        };
    }

    public static class Config {
        // Configuration properties if needed
    }
}