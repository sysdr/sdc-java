package com.example.logproducer.txshipper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {
    private String transactionId;
    private String userId;
    private String type;
    private BigDecimal amount;
    private String currency;
    private String status;
    private Instant timestamp;
}
