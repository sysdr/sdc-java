package com.example.sessionization.processor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionAggregate {
    private String userId;
    private String sessionId;
    private long startTime;
    private long endTime;
    private int eventCount;
    private Map<String, Integer> eventTypeCounts = new HashMap<>();
    private Set<String> pagesVisited = new HashSet<>();
    private boolean hasConversion;
    private String deviceType;
    private String location;
    private List<String> eventSequence = new ArrayList<>();
    
    public SessionAggregate addEvent(UserEvent event) {
        if (this.userId == null) {
            this.userId = event.getUserId();
            this.sessionId = event.getSessionId();
            this.startTime = event.getTimestamp();
            this.deviceType = event.getDeviceType();
            this.location = event.getLocation();
        }
        
        this.endTime = event.getTimestamp();
        this.eventCount++;
        
        // Track event types
        eventTypeCounts.merge(event.getEventType(), 1, Integer::sum);
        
        // Track pages
        if (event.getPage() != null) {
            pagesVisited.add(event.getPage());
        }
        
        // Track conversion
        if ("PURCHASE".equals(event.getEventType())) {
            this.hasConversion = true;
        }
        
        // Track event sequence (limit to last 20 events)
        eventSequence.add(event.getEventType());
        if (eventSequence.size() > 20) {
            eventSequence.remove(0);
        }
        
        return this;
    }
    
    public SessionAggregate merge(SessionAggregate other) {
        if (other == null) return this;
        
        this.eventCount += other.eventCount;
        this.endTime = Math.max(this.endTime, other.endTime);
        this.startTime = Math.min(this.startTime, other.startTime);
        
        // Merge event type counts
        other.eventTypeCounts.forEach((key, value) -> 
            this.eventTypeCounts.merge(key, value, Integer::sum));
        
        // Merge pages
        this.pagesVisited.addAll(other.pagesVisited);
        
        // Merge conversion status
        this.hasConversion = this.hasConversion || other.hasConversion;
        
        // Merge event sequences
        this.eventSequence.addAll(other.eventSequence);
        if (this.eventSequence.size() > 20) {
            this.eventSequence = new ArrayList<>(
                this.eventSequence.subList(this.eventSequence.size() - 20, this.eventSequence.size())
            );
        }
        
        return this;
    }
    
    public long getDurationSeconds() {
        return (endTime - startTime) / 1000;
    }
}
