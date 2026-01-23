package com.example.sessionization.processor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserEvent {
    private String eventId;
    private String userId;
    private String sessionId;
    private String eventType;
    private String page;
    private long timestamp;
    private String deviceType;
    private String location;
    private String metadata;
}
