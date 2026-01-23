package com.example.mapreduce.reducer.service;

import com.example.mapreduce.common.model.KeyValue;
import com.example.mapreduce.common.model.ReduceTask;
import com.example.mapreduce.reducer.entity.ResultEntity;
import com.example.mapreduce.reducer.repository.ResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReduceTaskExecutor {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ResultRepository resultRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    
    public ReduceTaskExecutor(RedisTemplate<String, String> redisTemplate,
                             KafkaTemplate<String, String> kafkaTemplate,
                             ResultRepository resultRepository,
                             ObjectMapper objectMapper,
                             MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.resultRepository = resultRepository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }
    
    @KafkaListener(topics = "reduce-tasks", groupId = "reduce-workers")
    public void executeReduceTask(String taskJson) {
        try {
            ReduceTask task = objectMapper.readValue(taskJson, ReduceTask.class);
            log.info("Executing reduce task {} for partition {}", task.getTaskId(), task.getPartition());
            
            // Read intermediate results from Redis
            String key = String.format("mr:intermediate:%s:%d", task.getJobId(), task.getPartition());
            List<String> intermediateData = redisTemplate.opsForList().range(key, 0, -1);
            
            if (intermediateData == null || intermediateData.isEmpty()) {
                log.warn("No intermediate data found for task {}", task.getTaskId());
                kafkaTemplate.send("task-completions", task.getTaskId(), task.getTaskId() + ":COMPLETED");
                return;
            }
            
            // Parse KeyValue pairs
            List<KeyValue> kvPairs = intermediateData.stream()
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, KeyValue.class);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
            
            // Group by key and reduce
            Map<String, List<String>> grouped = kvPairs.stream()
                .collect(Collectors.groupingBy(
                    KeyValue::getKey,
                    Collectors.mapping(KeyValue::getValue, Collectors.toList())
                ));
            
            // Apply reduce function (sum for word count)
            List<ResultEntity> results = grouped.entrySet().stream()
                .map(entry -> {
                    long sum = entry.getValue().stream()
                        .mapToLong(Long::parseLong)
                        .sum();
                    
                    ResultEntity result = new ResultEntity();
                    result.setJobId(task.getJobId());
                    result.setResultKey(entry.getKey());
                    result.setResultValue(sum);
                    return result;
                })
                .collect(Collectors.toList());
            
            // Save to PostgreSQL
            resultRepository.saveAll(results);
            
            // Clean up intermediate data
            redisTemplate.delete(key);
            
            // Report completion
            kafkaTemplate.send("task-completions", task.getTaskId(), task.getTaskId() + ":COMPLETED");
            
            log.info("Completed reduce task {} with {} unique keys", 
                task.getTaskId(), results.size());
            
        } catch (Exception e) {
            log.error("Reduce task execution failed", e);
        }
    }
}
