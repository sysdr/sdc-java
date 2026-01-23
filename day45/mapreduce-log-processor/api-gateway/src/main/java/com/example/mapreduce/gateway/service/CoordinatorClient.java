package com.example.mapreduce.gateway.service;

import com.example.mapreduce.common.model.Job;
import com.example.mapreduce.gateway.dto.JobRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoordinatorClient {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String COORDINATOR_URL = "http://localhost:8080/api/coordinator";
    
    public Job submitJob(JobRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = new HashMap<>();
            body.put("jobName", request.getJobName());
            body.put("inputTopic", request.getInputTopic());
            body.put("numMappers", request.getNumMappers());
            body.put("numReducers", request.getNumReducers());
            body.put("mapFunction", request.getMapFunction());
            body.put("reduceFunction", request.getReduceFunction());
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            String response = restTemplate.postForObject(
                COORDINATOR_URL + "/jobs",
                entity,
                String.class
            );
            
            return objectMapper.readValue(response, Job.class);
        } catch (Exception e) {
            log.error("Failed to submit job to coordinator", e);
            throw new RuntimeException("Job submission failed", e);
        }
    }
    
    public Job getJob(String jobId) {
        try {
            String response = restTemplate.getForObject(
                COORDINATOR_URL + "/jobs/" + jobId,
                String.class
            );
            return objectMapper.readValue(response, Job.class);
        } catch (Exception e) {
            log.error("Failed to get job from coordinator", e);
            return null;
        }
    }
}
