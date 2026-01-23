package com.example.mapreduce.mapper.service;

import com.example.mapreduce.common.model.KeyValue;
import com.example.mapreduce.common.model.LogEvent;
import com.example.mapreduce.common.model.MapTask;
import com.example.mapreduce.common.util.HashPartitioner;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MapTaskExecutor {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Counter shuffleBytesTotal;
    
    public MapTaskExecutor(RedisTemplate<String, String> redisTemplate,
                          KafkaTemplate<String, String> kafkaTemplate,
                          ObjectMapper objectMapper,
                          MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.shuffleBytesTotal = Counter.builder("mapreduce_shuffle_bytes_total")
            .description("Total bytes shuffled to Redis")
            .register(meterRegistry);
    }
    
    @KafkaListener(topics = "map-tasks", groupId = "map-workers")
    public void executeMapTask(String taskJson) {
        try {
            MapTask task = objectMapper.readValue(taskJson, MapTask.class);
            log.info("Executing map task {} for partition {}", task.getTaskId(), task.getPartition());
            
            // Simulate reading logs from Kafka partition
            List<LogEvent> logs = readLogsFromPartition(task.getPartition());
            
            // Apply map function (word count)
            List<KeyValue> mappedResults = applyMapFunction(logs, task.getMapFunction());
            
            // Partition results by reduce key
            Map<Integer, List<KeyValue>> partitions = partitionResults(mappedResults, task.getNumReducers());
            
            // Write to Redis
            writeIntermediateResults(task.getJobId(), partitions);
            
            // Report completion
            kafkaTemplate.send("task-completions", task.getTaskId(), task.getTaskId() + ":COMPLETED");
            
            log.info("Completed map task {} with {} output records", 
                task.getTaskId(), mappedResults.size());
            
        } catch (Exception e) {
            log.error("Map task execution failed", e);
        }
    }
    
    private List<LogEvent> readLogsFromPartition(int partition) {
        // Simulate reading logs - in production, would poll Kafka
        List<LogEvent> logs = new ArrayList<>();
        Random random = new Random(partition);
        
        String[] levels = {"ERROR", "WARN", "INFO", "DEBUG"};
        String[] services = {"api-gateway", "auth-service", "user-service", "payment-service"};
        String[] messages = {
            "Connection timeout to database",
            "User authentication failed",
            "Request processed successfully",
            "Cache miss for key user_12345",
            "Rate limit exceeded",
            "Invalid input parameter",
            "Service unavailable",
            "Processing payment transaction"
        };
        
        for (int i = 0; i < 1000; i++) {
            LogEvent log = new LogEvent();
            log.setId(UUID.randomUUID().toString());
            log.setLevel(levels[random.nextInt(levels.length)]);
            log.setService(services[random.nextInt(services.length)]);
            log.setMessage(messages[random.nextInt(messages.length)]);
            log.setTimestamp(java.time.Instant.now());
            logs.add(log);
        }
        
        return logs;
    }
    
    private List<KeyValue> applyMapFunction(List<LogEvent> logs, String mapFunction) {
        List<KeyValue> results = new ArrayList<>();
        
        // Word count map function
        for (LogEvent log : logs) {
            String[] words = log.getMessage().toLowerCase().split("\\s+");
            for (String word : words) {
                word = word.replaceAll("[^a-z0-9]", "");
                if (!word.isEmpty()) {
                    results.add(new KeyValue(word, "1"));
                }
            }
            
            // Also emit pattern: service -> level count
            results.add(new KeyValue(
                "pattern:" + log.getService() + ":" + log.getLevel(),
                "1"
            ));
        }
        
        return results;
    }
    
    private Map<Integer, List<KeyValue>> partitionResults(List<KeyValue> results, int numReducers) {
        Map<Integer, List<KeyValue>> partitions = new HashMap<>();
        
        for (KeyValue kv : results) {
            int partition = HashPartitioner.getPartition(kv.getKey(), numReducers);
            partitions.computeIfAbsent(partition, k -> new ArrayList<>()).add(kv);
        }
        
        return partitions;
    }
    
    private void writeIntermediateResults(String jobId, Map<Integer, List<KeyValue>> partitions) {
        partitions.forEach((partition, kvList) -> {
            try {
                String key = String.format("mr:intermediate:%s:%d", jobId, partition);
                
                List<String> serialized = kvList.stream()
                    .map(kv -> {
                        try {
                            return objectMapper.writeValueAsString(kv);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());
                
                redisTemplate.opsForList().rightPushAll(key, serialized);
                redisTemplate.expire(key, Duration.ofHours(1));
                
                // Track shuffle bytes
                long bytesShuffled = serialized.stream()
                    .mapToLong(String::length)
                    .sum();
                shuffleBytesTotal.increment(bytesShuffled);
                
            } catch (Exception e) {
                log.error("Failed to write intermediate results", e);
            }
        });
    }
}
