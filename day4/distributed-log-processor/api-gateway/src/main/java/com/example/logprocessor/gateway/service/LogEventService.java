package com.example.logprocessor.gateway.service;

import com.example.logprocessor.gateway.entity.LogEvent;
import com.example.logprocessor.gateway.repository.LogEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LogEventService {
    
    private static final Logger logger = LoggerFactory.getLogger(LogEventService.class);
    
    @Autowired
    private LogEventRepository logEventRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private final Counter eventsStoredCounter;
    
    public LogEventService(MeterRegistry meterRegistry) {
        this.eventsStoredCounter = Counter.builder("log_events_stored_total")
            .description("Total number of log events stored in database")
            .register(meterRegistry);
    }
    
    @KafkaListener(topics = "${app.kafka.topic.parsed-events}")
    public void storeParsedEvent(String eventJson) {
        try {
            JsonNode eventNode = objectMapper.readTree(eventJson);
            LogEvent logEvent = convertToEntity(eventNode);
            logEventRepository.save(logEvent);
            eventsStoredCounter.increment();
            logger.debug("Stored log event: {}", logEvent.getId());
        } catch (Exception e) {
            logger.error("Failed to store parsed event: {}", eventJson, e);
        }
    }
    
    private LogEvent convertToEntity(JsonNode eventNode) {
        LogEvent logEvent = new LogEvent();
        
        if (eventNode.has("timestamp")) {
            try {
                LocalDateTime timestamp = LocalDateTime.parse(
                    eventNode.get("timestamp").asText(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                );
                logEvent.setTimestamp(timestamp);
            } catch (Exception e) {
                logger.warn("Failed to parse timestamp, using current time", e);
                logEvent.setTimestamp(LocalDateTime.now());
            }
        }
        
        if (eventNode.has("ip_address")) {
            logEvent.setIpAddress(eventNode.get("ip_address").asText());
        }
        
        if (eventNode.has("http_method")) {
            logEvent.setHttpMethod(eventNode.get("http_method").asText());
        }
        
        if (eventNode.has("request_path")) {
            logEvent.setRequestPath(eventNode.get("request_path").asText());
        }
        
        if (eventNode.has("status_code")) {
            logEvent.setStatusCode(eventNode.get("status_code").asInt());
        }
        
        if (eventNode.has("response_size")) {
            logEvent.setResponseSize(eventNode.get("response_size").asLong());
        }
        
        if (eventNode.has("user_agent")) {
            logEvent.setUserAgent(eventNode.get("user_agent").asText());
        }
        
        if (eventNode.has("referer")) {
            logEvent.setReferer(eventNode.get("referer").asText());
        }
        
        if (eventNode.has("response_time")) {
            logEvent.setResponseTime(eventNode.get("response_time").asDouble());
        }
        
        if (eventNode.has("log_format")) {
            logEvent.setLogFormat(eventNode.get("log_format").asText());
        }
        
        if (eventNode.has("raw_log")) {
            logEvent.setRawLog(eventNode.get("raw_log").asText());
        }
        
        return logEvent;
    }
    
    public Page<LogEvent> getLogEvents(Pageable pageable) {
        return logEventRepository.findAll(pageable);
    }
    
    public Page<LogEvent> getLogEventsByIp(String ipAddress, Pageable pageable) {
        return logEventRepository.findByIpAddress(ipAddress, pageable);
    }
    
    public Page<LogEvent> getLogEventsByStatus(Integer statusCode, Pageable pageable) {
        return logEventRepository.findByStatusCode(statusCode, pageable);
    }
    
    public List<Map<String, Object>> getStatusCodeStats() {
        List<Object[]> results = logEventRepository.getStatusCodeCounts();
        return results.stream()
            .map(row -> Map.of("statusCode", row[0], "count", row[1]))
            .collect(Collectors.toList());
    }
    
    public List<Map<String, Object>> getHourlyStats() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<Object[]> results = logEventRepository.getHourlyRequestCounts(since);
        return results.stream()
            .map(row -> Map.of("hour", row[0], "count", row[1]))
            .collect(Collectors.toList());
    }
}
