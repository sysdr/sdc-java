package com.example.mapreduce.mapper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskCompletionListener {
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @KafkaListener(topics = "task-completions", groupId = "completion-listeners")
    public void handleTaskCompletion(String message) {
        try {
            String[] parts = message.split(":");
            String taskId = parts[0];
            String status = parts[1];
            
            // Notify coordinator
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String url = "http://localhost:8080/api/coordinator/tasks/" + taskId + "/complete";
            restTemplate.postForEntity(url, new HttpEntity<>(headers), Void.class);
            
            log.info("Notified coordinator of task {} completion", taskId);
        } catch (Exception e) {
            log.error("Failed to notify coordinator", e);
        }
    }
}
