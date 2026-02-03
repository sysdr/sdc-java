package com.example.logprocessor.consumer.service;

import com.example.logprocessor.consumer.model.LogEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Watermark-based reorder buffer.
 *
 * Problem: After cross-region replication, events arrive out of their
 *          original production order. A consumer reading the merged
 *          stream sees interleaved timestamps from both regions.
 *
 * Solution: Hold events in a sorted buffer keyed by eventTimestamp.
 *           Maintain a watermark (the oldest timestamp we're still
 *           willing to accept). Events older than watermark - windowSize
 *           are flushed in order. Events that arrive after the watermark
 *           has advanced past their timestamp are "late" and routed
 *           to the dead-letter topic.
 *
 * Trade-off:
 *   - Small window (< 500ms): low latency, higher late-arrival rate
 *   - Large window (> 2s):    better ordering, higher end-to-end latency
 *
 * The window is tunable via application.yml without code changes.
 */
@Service
public class ReorderBufferService {

    private static final Logger log = LoggerFactory.getLogger(ReorderBufferService.class);

    /** Thread-safe sorted map: eventTimestamp -> list of events at that timestamp */
    private final ConcurrentSkipListMap<Instant, List<LogEvent>> buffer = new ConcurrentSkipListMap<>();

    private final long windowMillis;
    private volatile Instant watermark = Instant.now();

    private final Counter lateArrivalCounter;
    private final Gauge bufferSizeGauge;

    public ReorderBufferService(
            @Value("${app.reorder.window-ms:1000}") long windowMillis,
            @Value("${app.region}") String region,
            MeterRegistry meterRegistry
    ) {
        this.windowMillis = windowMillis;
        this.lateArrivalCounter = meterRegistry.counter("reorder.late.arrivals", "region", region);
        this.bufferSizeGauge = Gauge.builder("reorder.buffer.size", this, s -> (double) s.buffer.size())
                .register(meterRegistry);
    }

    /**
     * Adds an event to the reorder buffer.
     * If the event's timestamp is older than the current watermark minus the window,
     * it is immediately classified as a late arrival.
     *
     * @return true if the event was buffered normally; false if it is a late arrival
     */
    public boolean add(LogEvent event) {
        Instant cutoff = watermark.minusMillis(windowMillis);

        if (event.eventTimestamp().isBefore(cutoff)) {
            lateArrivalCounter.increment();
            log.warn("Late arrival detected: eventId={} timestamp={} cutoff={}",
                    event.eventId(), event.eventTimestamp(), cutoff);
            return false; // Caller routes this to DLQ
        }

        buffer.computeIfAbsent(event.eventTimestamp(), k -> new ArrayList<>()).add(event);

        // Advance watermark to the latest event timestamp seen
        if (event.eventTimestamp().isAfter(watermark)) {
            watermark = event.eventTimestamp();
        }

        return true;
    }

    /**
     * Flushes all events that are older than (watermark - windowSize).
     * Returns them sorted by eventTimestamp ascending.
     * Should be called periodically (e.g., every 200ms) by the consumer loop.
     */
    public List<LogEvent> flush() {
        Instant cutoff = watermark.minusMillis(windowMillis);
        List<LogEvent> flushed = new ArrayList<>();

        // headMap is exclusive of cutoff; all entries strictly before cutoff are safe to emit
        NavigableMap<Instant, List<LogEvent>> readyEntries = buffer.headMap(cutoff, false);
        for (Map.Entry<Instant, List<LogEvent>> entry : new ArrayList<>(readyEntries.entrySet())) {
            flushed.addAll(entry.getValue());
            buffer.remove(entry.getKey());
        }

        return flushed; // Already sorted by timestamp due to ConcurrentSkipListMap
    }

    /** Current number of events held in the buffer */
    public int size() {
        return buffer.values().stream().mapToInt(List::size).sum();
    }
}
