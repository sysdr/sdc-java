package com.example.logconsumer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "idempotency_keys", indexes = {
    @Index(name = "idx_processed_at", columnList = "processedAt")
})
public class IdempotencyKey {
    @Id
    private String messageId;
    
    private Instant processedAt;
    private String consumerGroup;
}
