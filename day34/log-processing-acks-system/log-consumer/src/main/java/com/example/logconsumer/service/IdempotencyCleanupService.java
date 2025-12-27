package com.example.logconsumer.service;

import com.example.logconsumer.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdempotencyCleanupService {

    private final IdempotencyKeyRepository idempotencyRepository;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldKeys() {
        Instant cutoffTime = Instant.now().minus(7, ChronoUnit.DAYS);
        int deleted = idempotencyRepository.deleteOlderThan(cutoffTime);
        log.info("Cleaned up {} idempotency keys older than {}", deleted, cutoffTime);
    }
}
