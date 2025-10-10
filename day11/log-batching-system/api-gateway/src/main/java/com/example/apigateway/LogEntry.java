package com.example.apigateway;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "log_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {
    @Id
    private String id;
    private String level;
    private String service;
    
    @Column(length = 1000)
    private String message;
    
    private String timestamp;
    private String traceId;
}
