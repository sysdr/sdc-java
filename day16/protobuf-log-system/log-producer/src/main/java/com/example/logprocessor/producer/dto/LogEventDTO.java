package com.example.logprocessor.producer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

@Data
public class LogEventDTO {
    @NotBlank(message = "Event ID is required")
    private String eventId;
    
    @NotBlank(message = "Timestamp is required")
    private String timestamp;
    
    @NotNull(message = "Log level is required")
    private String level;
    
    @NotBlank(message = "Message is required")
    private String message;
    
    @NotBlank(message = "Service name is required")
    private String serviceName;
    
    private String host;
    private String environment;
    private Map<String, String> tags;
    private String traceId;
    private String spanId;
    private Map<String, String> customFields;
}
