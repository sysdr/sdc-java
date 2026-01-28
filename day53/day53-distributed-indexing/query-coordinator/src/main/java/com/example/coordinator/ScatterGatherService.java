package com.example.coordinator;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScatterGatherService {
    private static final Logger log = LoggerFactory.getLogger(ScatterGatherService.class);

    private final WebClient webClient;
    private final List<String> indexNodes;
    private final int queryTimeoutMs;
    private final Counter queriesCounter;
    private final Counter partialResultsCounter;
    private final Timer scatterGatherTimer;

    public ScatterGatherService(
            WebClient.Builder webClientBuilder,
            @Value("${index.nodes}") String indexNodesConfig,
            @Value("${query.timeout.ms:500}") int queryTimeoutMs,
            MeterRegistry meterRegistry) {
        
        this.webClient = webClientBuilder.build();
        this.indexNodes = Arrays.asList(indexNodesConfig.split(","));
        this.queryTimeoutMs = queryTimeoutMs;

        this.queriesCounter = Counter.builder("coordinator.queries.total")
                .description("Total queries coordinated")
                .register(meterRegistry);
        this.partialResultsCounter = Counter.builder("coordinator.queries.partial")
                .description("Queries with partial results")
                .register(meterRegistry);
        this.scatterGatherTimer = Timer.builder("coordinator.scatter.gather.duration")
                .description("Time to scatter-gather across all shards")
                .register(meterRegistry);

        log.info("Query coordinator initialized with {} index nodes: {}", 
                indexNodes.size(), indexNodes);
    }

    public MergedSearchResult search(String query, int limit) {
        return scatterGatherTimer.record(() -> {
            queriesCounter.increment();
            long startTime = System.currentTimeMillis();

            log.info("Scattering query '{}' to {} nodes", query, indexNodes.size());

            // Scatter: Send query to all nodes in parallel
            List<Mono<SearchResult>> searches = indexNodes.stream()
                    .map(nodeUrl -> searchNode(nodeUrl, query, limit))
                    .collect(Collectors.toList());

            // Gather: Collect results with timeout
            List<SearchResult> results = Flux.merge(searches)
                    .timeout(Duration.ofMillis(queryTimeoutMs))
                    .onErrorResume(e -> {
                        log.warn("Node query failed: {}", e.getMessage());
                        return Mono.empty();
                    })
                    .collectList()
                    .block();

            if (results == null) {
                results = new ArrayList<>();
            }

            // Merge results
            MergedSearchResult merged = mergeResults(results, limit, startTime);
            
            if (merged.isPartial()) {
                partialResultsCounter.increment();
                log.warn("Query completed with partial results: {}/{} shards succeeded", 
                        merged.getShardsSucceeded(), merged.getShardsQueried());
            }

            log.info("Query '{}' completed: {} total hits from {}/{} shards in {}ms",
                    query, merged.getTotalHits(), merged.getShardsSucceeded(), 
                    merged.getShardsQueried(), merged.getSearchTimeMs());

            return merged;
        });
    }

    private Mono<SearchResult> searchNode(String nodeUrl, String query, int limit) {
        return webClient.get()
                .uri(nodeUrl + "/api/search?q={query}&limit={limit}", query, limit)
                .retrieve()
                .bodyToMono(SearchResult.class)
                .doOnSuccess(result -> log.debug("Node {} returned {} hits", 
                        nodeUrl, result.getTotalHits()))
                .doOnError(e -> log.error("Node {} search failed: {}", nodeUrl, e.getMessage()));
    }

    private MergedSearchResult mergeResults(List<SearchResult> results, int limit, long startTime) {
        // Use a max-heap to keep top-K results by timestamp (most recent first)
        PriorityQueue<LogEntry> topK = new PriorityQueue<>(
                limit,
                Comparator.comparingLong(LogEntry::getTimestamp).reversed()
        );

        int totalHits = 0;
        for (SearchResult result : results) {
            totalHits += result.getTotalHits();
            for (LogEntry log : result.getLogs()) {
                topK.offer(log);
                if (topK.size() > limit) {
                    topK.poll();
                }
            }
        }

        // Extract sorted results
        List<LogEntry> mergedLogs = new ArrayList<>(topK);
        mergedLogs.sort(Comparator.comparingLong(LogEntry::getTimestamp).reversed());

        long searchTime = System.currentTimeMillis() - startTime;
        boolean partial = results.size() < indexNodes.size();

        return new MergedSearchResult(
                mergedLogs,
                totalHits,
                searchTime,
                indexNodes.size(),
                results.size(),
                partial
        );
    }
}
