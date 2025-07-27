package com.beaver.core.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class RateLimitConfig {

    /**
     * Rate limiting based on IP address for authentication endpoints
     * This prevents brute force attacks and abuse of auth endpoints
     */
    @Bean
    public KeyResolver ipAddressKeyResolver() {
        return exchange -> {
            String clientIp = exchange.getRequest().getRemoteAddress() != null 
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
            return Mono.just(clientIp);
        };
    }

    /**
     * In-memory rate limiter as fallback when Redis is not available
     * This provides basic rate limiting functionality for authentication endpoints
     */
    @Bean
    public RateLimiter inMemoryRateLimiter() {
        return new InMemoryRateLimiter();
    }
    
    /**
     * Simple in-memory rate limiter implementation
     * Uses sliding window approach with configurable rates
     */
    static class InMemoryRateLimiter implements RateLimiter<Object> {
        
        private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
        private final int capacity = 10;
        private final int refillRate = 5;
        
        @Override
        public Mono<Response> isAllowed(String routeId, String id) {
            TokenBucket bucket = buckets.computeIfAbsent(id, k -> new TokenBucket(capacity, refillRate));
            
            if (bucket.tryConsume()) {
                return Mono.just(new Response(true, java.util.Map.of()));
            } else {
                return Mono.just(new Response(false,
                    java.util.Map.of("X-RateLimit-Retry-After-Seconds", "1")));
            }
        }
        
        @Override
        public java.util.Map<String, Object> getConfig() {
            return java.util.Map.of(
                "capacity", capacity,
                "refillRate", refillRate
            );
        }
        
        @Override
        public Class<Object> getConfigClass() {
            return Object.class;
        }
        
        @Override
        public Object newConfig() {
            return new Object();
        }
        
        /**
         * Simple token bucket implementation for rate limiting
         */
        static class TokenBucket {
            private final int capacity;
            private final int refillRate;
            private final AtomicInteger tokens;
            private final AtomicLong lastRefill;
            
            TokenBucket(int capacity, int refillRate) {
                this.capacity = capacity;
                this.refillRate = refillRate;
                this.tokens = new AtomicInteger(capacity);
                this.lastRefill = new AtomicLong(System.currentTimeMillis());
            }
            
            boolean tryConsume() {
                refill();
                return tokens.getAndUpdate(current -> current > 0 ? current - 1 : current) > 0;
            }
            
            private void refill() {
                long now = System.currentTimeMillis();
                long lastRefillTime = lastRefill.get();
                long timePassed = now - lastRefillTime;
                
                if (timePassed > 1000) {
                    int tokensToAdd = (int) (timePassed / 1000) * refillRate;
                    if (tokensToAdd > 0 && lastRefill.compareAndSet(lastRefillTime, now)) {
                        tokens.updateAndGet(current -> Math.min(capacity, current + tokensToAdd));
                    }
                }
            }
        }
    }
}