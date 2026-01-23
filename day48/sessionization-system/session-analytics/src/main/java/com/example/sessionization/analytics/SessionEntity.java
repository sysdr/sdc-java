package com.example.sessionization.analytics;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "user_sessions", indexes = {
    @Index(name = "idx_user_start_time", columnList = "userId,startTime"),
    @Index(name = "idx_converted", columnList = "hasConversion")
})
@Data
@NoArgsConstructor
public class SessionEntity {
    @Id
    private String sessionId;
    
    private String userId;
    
    @Column(nullable = false)
    private Instant startTime;
    
    @Column(nullable = false)
    private Instant endTime;
    
    private long durationSeconds;
    
    private int eventCount;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Integer> eventTypeCounts;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Set<String> pagesVisited;
    
    private boolean hasConversion;
    
    private String deviceType;
    
    private String location;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
