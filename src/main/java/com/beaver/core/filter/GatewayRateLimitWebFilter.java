package com.beaver.core.filter;

import com.beaver.core.exception.RateLimitExceededException;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class GatewayRateLimitWebFilter implements WebFilter {

    private final RateLimiter<Object> rateLimiter;

    public GatewayRateLimitWebFilter(RateLimiter<Object> rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String clientIp = getClientIp(exchange);
        
        return rateLimiter.isAllowed("gateway", clientIp)
                .flatMap(response -> {
                    if (response.isAllowed()) {
                        return chain.filter(exchange);
                    } else {
                        return Mono.error(new RateLimitExceededException("Too many requests. Please slow down."));
                    }
                });
    }

    private String getClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return exchange.getRequest().getRemoteAddress() != null
            ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
            : "unknown";
    }
}
