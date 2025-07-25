package com.beaver.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.findAndRegisterModules();

        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL
        );

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer));

        // Specific cache configurations for different entity types
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Users cache - 30 minutes TTL (same as default, but explicit)
        cacheConfigurations.put("users", defaultConfig);

        // Only add these if you need different TTL than the 30-minute default:

        // Accounts cache - 15 minutes TTL (shorter because data changes more frequently)
        // cacheConfigurations.put("accounts", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Products cache - 2 hours TTL (longer because product data is more stable)
        // cacheConfigurations.put("products", defaultConfig.entryTtl(Duration.ofHours(2)));

        // Sessions/tokens cache - 5 minutes TTL (security-sensitive, short lived)
        // cacheConfigurations.put("sessions", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Settings cache - 24 hours TTL (rarely changes)
        // cacheConfigurations.put("settings", defaultConfig.entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
