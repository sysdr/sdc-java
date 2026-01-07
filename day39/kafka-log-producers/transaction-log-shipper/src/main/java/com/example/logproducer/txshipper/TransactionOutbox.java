package com.example.logproducer.txshipper;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transaction_outbox")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionOutbox {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String transactionId;
    
    private String userId;
    private BigDecimal amount;
    private String currency;
    private String status;
    
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    private Instant sentAt;
}
