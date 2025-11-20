package com.example.schemaregistry.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(24))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        
        // Schemas by ID - cache for 24 hours (schemas are immutable)
        cacheConfigs.put("schemas", defaultConfig.entryTtl(Duration.ofHours(24)));
        
        // Schema ID lookups - cache for 24 hours
        cacheConfigs.put("schema-ids", defaultConfig.entryTtl(Duration.ofHours(24)));
        
        // Latest versions - shorter TTL for fresher data
        cacheConfigs.put("latest-versions", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // All schemas by subject
        cacheConfigs.put("subject-schemas", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }
}
