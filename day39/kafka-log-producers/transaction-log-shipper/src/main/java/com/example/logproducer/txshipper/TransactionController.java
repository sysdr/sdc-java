package com.example.logproducer.txshipper;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {
    
    private final IdempotentProducerService producerService;
    
    @PostMapping
    public ResponseEntity<Map<String, String>> createTransaction(@RequestBody Map<String, Object> request) {
        try {
            TransactionEvent event = TransactionEvent.builder()
                .transactionId(UUID.randomUUID().toString())
                .userId((String) request.get("userId"))
                .type((String) request.getOrDefault("type", "PAYMENT"))
                .amount(new BigDecimal(request.get("amount").toString()))
                .currency((String) request.getOrDefault("currency", "USD"))
                .status("PENDING")
                .timestamp(Instant.now())
                .build();
            
            producerService.sendTransaction(event);
            
            return ResponseEntity.ok(Map.of(
                "status", "accepted",
                "transactionId", event.getTransactionId()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
}
