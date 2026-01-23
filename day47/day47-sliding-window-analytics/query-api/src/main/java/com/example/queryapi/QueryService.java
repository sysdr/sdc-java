package com.example.queryapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class QueryService {
    private static final Logger logger = LoggerFactory.getLogger(QueryService.class);
    
    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Timer queryTimer;
    
    public QueryService(StreamsBuilderFactoryBean streamsBuilderFactoryBean,
                       RedisTemplate<String, String> redisTemplate,
                       ObjectMapper objectMapper,
                       MeterRegistry meterRegistry) {
        this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheHitCounter = meterRegistry.counter("query.cache.hit");
        this.cacheMissCounter = meterRegistry.counter("query.cache.miss");
        this.queryTimer = meterRegistry.timer("query.latency");
    }
    
    public TrendResponse getTrends(String serviceId) {
        return queryTimer.record(() -> {
            // Check cache first
            String cacheKey = "trends:" + serviceId;
            String cached = redisTemplate.opsForValue().get(cacheKey);
            
            if (cached != null) {
                try {
                    cacheHitCounter.increment();
                    TrendResponse response = objectMapper.readValue(cached, TrendResponse.class);
                    response.setFromCache(true);
                    logger.debug("Cache hit for service: {}", serviceId);
                    return response;
                } catch (Exception e) {
                    logger.warn("Failed to deserialize cached data", e);
                }
            }
            
            cacheMissCounter.increment();
            logger.debug("Cache miss for service: {}", serviceId);
            
            // Query state stores
            TrendResponse response = queryStateStores(serviceId);
            
            // Cache the result for 10 seconds (same as hop interval)
            try {
                String json = objectMapper.writeValueAsString(response);
                redisTemplate.opsForValue().set(cacheKey, json, Duration.ofSeconds(10));
            } catch (Exception e) {
                logger.error("Failed to cache result", e);
            }
            
            return response;
        });
    }
    
    private TrendResponse queryStateStores(String serviceId) {
        KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
        
        if (kafkaStreams == null || !kafkaStreams.state().isRunningOrRebalancing()) {
            logger.warn("Kafka Streams not ready");
            return TrendResponse.builder()
                .serviceId(serviceId)
                .timestamp(System.currentTimeMillis())
                .fromCache(false)
                .build();
        }
        
        long now = System.currentTimeMillis();
        
        // Query each window store
        WindowStats oneMinStats = queryWindow("one-min-windows", serviceId, now, Duration.ofMinutes(1));
        WindowStats fiveMinStats = queryWindow("five-min-windows", serviceId, now, Duration.ofMinutes(5));
        WindowStats fifteenMinStats = queryWindow("fifteen-min-windows", serviceId, now, Duration.ofMinutes(15));
        
        return TrendResponse.builder()
            .serviceId(serviceId)
            .oneMinAvgLatency(oneMinStats != null ? oneMinStats.getAvgLatency() : 0.0)
            .fiveMinAvgLatency(fiveMinStats != null ? fiveMinStats.getAvgLatency() : 0.0)
            .fifteenMinAvgLatency(fifteenMinStats != null ? fifteenMinStats.getAvgLatency() : 0.0)
            .oneMinAvgErrorRate(oneMinStats != null ? oneMinStats.getAvgErrorRate() : 0.0)
            .fiveMinAvgErrorRate(fiveMinStats != null ? fiveMinStats.getAvgErrorRate() : 0.0)
            .fifteenMinAvgErrorRate(fifteenMinStats != null ? fifteenMinStats.getAvgErrorRate() : 0.0)
            .oneMinThroughput(oneMinStats != null ? oneMinStats.getAvgThroughput() : 0.0)
            .fiveMinThroughput(fiveMinStats != null ? fiveMinStats.getAvgThroughput() : 0.0)
            .fifteenMinThroughput(fifteenMinStats != null ? fifteenMinStats.getAvgThroughput() : 0.0)
            .timestamp(now)
            .fromCache(false)
            .build();
    }
    
    private WindowStats queryWindow(String storeName, String key, long timestamp, Duration windowSize) {
        try {
            KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
            
            ReadOnlyWindowStore<String, WindowStats> windowStore = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType(storeName, QueryableStoreTypes.windowStore())
            );
            
            long from = timestamp - windowSize.toMillis();
            long to = timestamp;
            
            // Fetch all windows in the range and get the most recent one
            WindowStats latestStats = null;
            try (WindowStoreIterator<WindowStats> iterator = windowStore.fetch(key, Instant.ofEpochMilli(from), Instant.ofEpochMilli(to))) {
                while (iterator.hasNext()) {
                    KeyValue<Long, WindowStats> next = iterator.next();
                    latestStats = next.value;
                }
            }
            
            return latestStats;
        } catch (Exception e) {
            logger.error("Failed to query window store: {}", storeName, e);
            return null;
        }
    }
}
