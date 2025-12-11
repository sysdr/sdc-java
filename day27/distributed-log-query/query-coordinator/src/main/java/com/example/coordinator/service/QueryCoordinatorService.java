package com.example.coordinator.service;

import com.example.coordinator.model.LogEntry;
import com.example.coordinator.model.LogQuery;
import com.example.coordinator.model.PartitionMetadata;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QueryCoordinatorService {
    
    private final PartitionMetadataCache metadataCache;
    private final WebClient.Builder webClientBuilder;
    private final Timer queryTimer;
    private final Counter partitionQueryCounter;
    private final Counter partitionPruneCounter;
    
    public QueryCoordinatorService(
            PartitionMetadataCache metadataCache,
            WebClient.Builder webClientBuilder,
            MeterRegistry meterRegistry) {
        this.metadataCache = metadataCache;
        this.webClientBuilder = webClientBuilder;
        this.queryTimer = Timer.builder("query.execution.time")
            .description("Time taken to execute queries")
            .publishPercentileHistogram(true)  // Enable histogram for percentile queries
            .register(meterRegistry);
        this.partitionQueryCounter = Counter.builder("query.partitions.queried")
            .description("Number of partitions queried")
            .register(meterRegistry);
        this.partitionPruneCounter = Counter.builder("query.partitions.pruned")
            .description("Number of partitions pruned")
            .register(meterRegistry);
    }
    
    public Flux<LogEntry> executeQuery(LogQuery query) {
        return Flux.defer(() -> {
            long startTime = System.nanoTime();
            
            // Phase 1: Query Planning - Prune partitions
            Set<PartitionMetadata> targetPartitions = metadataCache.prunePartitions(query);
            int totalPartitions = metadataCache.getAllPartitions().size();
            int prunedCount = totalPartitions - targetPartitions.size();
            
            partitionPruneCounter.increment(prunedCount);
            partitionQueryCounter.increment(targetPartitions.size());
            
            log.info("Query planning: {} partitions queried, {} pruned out of {}",
                targetPartitions.size(), prunedCount, totalPartitions);
            
            if (targetPartitions.isEmpty()) {
                return Flux.empty();
            }
            
            // Phase 2: Scatter - Parallel queries to partitions
            List<Flux<LogEntry>> partitionStreams = targetPartitions.stream()
                .map(partition -> queryPartition(partition, query))
                .collect(Collectors.toList());
            
            // Phase 3: Gather - Merge ordered streams
            Flux<LogEntry> mergedResults = mergeOrderedStreams(partitionStreams);
            
            // Apply limit for early termination
            return mergedResults
                .take(query.getLimit())
                .doOnComplete(() -> {
                    long duration = System.nanoTime() - startTime;
                    queryTimer.record(Duration.ofNanos(duration));
                    log.info("Query completed in {} ms", duration / 1_000_000);
                });
        });
    }
    
    private Flux<LogEntry> queryPartition(PartitionMetadata partition, LogQuery query) {
        WebClient client = webClientBuilder
            .baseUrl(partition.getNodeUrl())
            .build();
        
        return client.post()
            .uri("/api/partition/query")
            .bodyValue(query)
            .retrieve()
            .bodyToFlux(LogEntry.class)
            .timeout(Duration.ofSeconds(5))
            .onErrorResume(e -> {
                log.warn("Query failed for partition {}: {}", 
                    partition.getPartitionId(), e.getMessage());
                // Return empty on failure, don't fail entire query
                return Flux.empty();
            })
            .doOnSubscribe(s -> log.debug("Querying partition: {}", 
                partition.getPartitionId()));
    }
    
    private Flux<LogEntry> mergeOrderedStreams(List<Flux<LogEntry>> streams) {
        // Use priority queue to merge timestamp-ordered streams
        return Flux.create(sink -> {
            PriorityQueue<StreamHead> heap = new PriorityQueue<>(
                Comparator.comparing(h -> h.currentLog.getTimestamp())
            );
            
            List<Iterator<LogEntry>> iterators = streams.stream()
                .map(flux -> flux.toIterable().iterator())
                .collect(Collectors.toList());
            
            // Initialize heap with first element from each stream
            for (int i = 0; i < iterators.size(); i++) {
                Iterator<LogEntry> iter = iterators.get(i);
                if (iter.hasNext()) {
                    heap.offer(new StreamHead(i, iter.next(), iter));
                }
            }
            
            // Emit logs in global timestamp order
            while (!heap.isEmpty()) {
                StreamHead head = heap.poll();
                sink.next(head.currentLog);
                
                if (head.iterator.hasNext()) {
                    heap.offer(new StreamHead(
                        head.streamId, 
                        head.iterator.next(), 
                        head.iterator
                    ));
                }
            }
            
            sink.complete();
        });
    }
    
    private static class StreamHead {
        final int streamId;
        final LogEntry currentLog;
        final Iterator<LogEntry> iterator;
        
        StreamHead(int streamId, LogEntry currentLog, Iterator<LogEntry> iterator) {
            this.streamId = streamId;
            this.currentLog = currentLog;
            this.iterator = iterator;
        }
    }
}
