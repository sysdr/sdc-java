package com.example.logconsumer.repository;

import com.example.logconsumer.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {
    
    @Modifying
    @Query("DELETE FROM IdempotencyKey k WHERE k.processedAt < :cutoffTime")
    int deleteOlderThan(Instant cutoffTime);
}
