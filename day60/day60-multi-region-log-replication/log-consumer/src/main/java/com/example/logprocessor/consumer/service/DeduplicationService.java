package com.example.logprocessor.consumer.service;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Two-tier deduplication:
 *
 * Tier 1 – Local Bloom Filter (Guava)
 *   - O(1) probabilistic check, near-zero memory footprint.
 *   - False-positive rate configured at 0.01% (1 in 10,000).
 *   - If the bloom filter says "seen", we skip the event immediately.
 *
 * Tier 2 – Redis SETNX (exact check)
 *   - Called only when the bloom filter says "not seen".
 *   - Provides a cross-instance consistency layer: if another consumer
 *     instance already processed this eventId, Redis will reject it.
 *   - TTL = 24 hours (configurable). Events older than this are assumed
 *     to have been fully persisted and can be safely re-processed if needed.
 *
 * Anti-pattern avoided: using a single global hash map. Under high cardinality
 * (millions of events/day), a hash map consumes gigabytes of heap and causes
 * GC pauses. The bloom filter + TTL-based Redis check keeps memory bounded.
 */
@Service
public class DeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(DeduplicationService.class);

    private final BloomFilter<String> bloomFilter;
    private final RedisTemplate<String, String> redisTemplate;
    private final String region;
    private final Duration redisTtl;

    private final Counter dedupHits;
    private final Counter dedupMisses;
    private final AtomicLong bloomFilterSize = new AtomicLong(0);

    /**
     * Custom Guava Funnel for String event IDs.
     * Using the default Funnel<CharSequence> would work, but an explicit
     * implementation documents the hashing contract and avoids ambiguity.
     */
    private static final Funnel<String> EVENT_ID_FUNNEL = new Funnel<>() {
        @Override
        public void funnel(String eventId, PrimitiveSink into) {
            into.putBytes(eventId.getBytes(StandardCharsets.UTF_8));
        }
    };

    public DeduplicationService(
            RedisTemplate<String, String> redisTemplate,
            @Value("${app.region}") String region,
            @Value("${app.dedup.bloom.expected-insertions:1000000}") int expectedInsertions,
            @Value("${app.dedup.bloom.false-positive-rate:0.0001}") double falsePositiveRate,
            @Value("${app.dedup.redis.ttl-hours:24}") long ttlHours,
            MeterRegistry meterRegistry
    ) {
        this.redisTemplate = redisTemplate;
        this.region = region;
        this.redisTtl = Duration.ofHours(ttlHours);
        this.bloomFilter = BloomFilter.create(EVENT_ID_FUNNEL, expectedInsertions, falsePositiveRate);

        this.dedupHits = meterRegistry.counter("dedup.hits", "region", region);
        this.dedupMisses = meterRegistry.counter("dedup.misses", "region", region);
    }

    /**
     * Returns true if this eventId has already been seen (duplicate).
     * Side effect: registers the eventId in both tiers if it is new.
     */
    public boolean isDuplicate(String eventId) {
        // Tier 1: bloom filter (local, fast)
        if (bloomFilter.mightContain(eventId)) {
            dedupHits.increment();
            return true;
        }

        // Tier 2: Redis SETNX (cross-instance, exact)
        String redisKey = "dedup:" + region + ":" + eventId;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", redisTtl);

        if (isNew == null || !isNew) {
            // Another instance already claimed this event
            dedupHits.increment();
            bloomFilter.put(eventId); // Cache locally for future checks
            return true;
        }

        // Event is genuinely new — register in bloom filter for subsequent checks
        bloomFilter.put(eventId);
        bloomFilterSize.incrementAndGet();
        dedupMisses.increment();
        return false;
    }

    /** Exposed for monitoring dashboards */
    public long getBloomFilterApproximateSize() {
        return bloomFilterSize.get();
    }
}
