package com.example.logprocessor.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Log Consumer microservice.
 *
 * Responsibilities:
 *   1. Consume log events from both the local region topic AND the
 *      MirrorMaker-replicated topic from the remote region.
 *   2. Deduplicate using a bloom filter + Redis-backed exact store.
 *   3. Reorder events using a watermark-based buffer.
 *   4. Persist deduplicated, ordered events to PostgreSQL.
 */
@SpringBootApplication
public class LogConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogConsumerApplication.class, args);
    }
}
