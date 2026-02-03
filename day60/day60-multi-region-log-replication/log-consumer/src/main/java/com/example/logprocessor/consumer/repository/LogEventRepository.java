package com.example.logprocessor.consumer.repository;

import com.example.logprocessor.consumer.model.PersistedLogEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * JPA repository for log event persistence.
 *
 * saveOrIgnore uses a native query with ON CONFLICT DO NOTHING.
 * This is the database-level idempotency guard: even if the bloom filter
 * has a false negative and lets a duplicate through, the DB won't insert it twice.
 */
public interface LogEventRepository extends JpaRepository<PersistedLogEvent, String> {

    @Modifying
    @Query(nativeQuery = true, value =
            "INSERT INTO log_events (event_id, source_region, service_name, level, message, " +
            "event_timestamp, correlation_id, consumed_at, consuming_region) " +
            "VALUES (:#{#event.eventId}, :#{#event.sourceRegion}, :#{#event.serviceName}, " +
            ":#{#event.level}, :#{#event.message}, :#{#event.eventTimestamp}, " +
            ":#{#event.correlationId}, :#{#event.consumedAt}, :#{#event.consumingRegion}) " +
            "ON CONFLICT (event_id) DO NOTHING")
    void saveOrIgnore(@org.springframework.data.repository.query.Param("event") PersistedLogEvent event);
}
