package com.example.sessionization.producer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {
    private String eventId;
    private String userId;
    private String sessionId;
    private String eventType; // PAGE_VIEW, CLICK, ADD_TO_CART, PURCHASE, SEARCH
    private String page;
    private long timestamp;
    private String deviceType;
    private String location;
    private String metadata;
}
