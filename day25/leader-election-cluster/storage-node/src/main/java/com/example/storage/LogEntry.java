package com.example.storage;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "log_entries", indexes = {
    @Index(name = "idx_term_index", columnList = "term,logIndex"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long term;
    
    @Column(nullable = false)
    private Long logIndex;
    
    @Column(nullable = false, length = 1000)
    private String data;
    
    @Column(nullable = false)
    private Instant timestamp;
    
    @Column(nullable = false)
    private String source;
    
    @Column(nullable = false)
    private boolean committed;
}
