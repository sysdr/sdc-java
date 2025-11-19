package com.example.logprocessor.normalizer.service;

import com.example.logprocessor.common.format.LogFormat;
import com.example.logprocessor.common.model.*;
import com.example.logprocessor.normalizer.detector.FormatDetector;
import com.example.logprocessor.normalizer.handler.FormatHandler;
import com.example.logprocessor.normalizer.metrics.NormalizationMetrics;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NormalizationService {

    private final Map<LogFormat, FormatHandler> handlers;
    private final FormatDetector detector;
    private final NormalizationMetrics metrics;

    public NormalizationService(List<FormatHandler> handlerList, 
                                FormatDetector detector,
                                NormalizationMetrics metrics) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(FormatHandler::getFormat, h -> h));
        this.detector = detector;
        this.metrics = metrics;

        log.info("Initialized normalizer with {} format handlers: {}", 
                handlers.size(), handlers.keySet());
    }

    @CircuitBreaker(name = "normalization", fallbackMethod = "normalizeFallback")
    public NormalizationResult normalize(byte[] input, LogFormat targetFormat) {
        return normalize(input, targetFormat, null);
    }

    public NormalizationResult normalize(byte[] input, LogFormat targetFormat, 
                                         String contentTypeHint) {
        long startTime = System.currentTimeMillis();
        String id = UUID.randomUUID().toString();

        try {
            // Detect source format
            LogFormat sourceFormat = contentTypeHint != null 
                    ? detector.detect(input, contentTypeHint)
                    : detector.detect(input);

            // Short-circuit if formats match
            if (sourceFormat == targetFormat) {
                metrics.recordConversion(sourceFormat, targetFormat, true, 
                        System.currentTimeMillis() - startTime);
                return NormalizationResult.success(id, sourceFormat, targetFormat, input,
                        System.currentTimeMillis() - startTime);
            }

            // Get handlers
            FormatHandler sourceHandler = handlers.get(sourceFormat);
            FormatHandler targetHandler = handlers.get(targetFormat);

            if (sourceHandler == null || targetHandler == null) {
                String error = String.format("No handler for %s or %s", 
                        sourceFormat, targetFormat);
                metrics.recordConversion(sourceFormat, targetFormat, false, 
                        System.currentTimeMillis() - startTime);
                return NormalizationResult.failure(id, sourceFormat, targetFormat, error);
            }

            // Parse to canonical form
            CanonicalLog canonical = sourceHandler.parse(input);

            // Serialize to target format
            byte[] output = targetHandler.serialize(canonical);

            long duration = System.currentTimeMillis() - startTime;
            metrics.recordConversion(sourceFormat, targetFormat, true, duration);

            return NormalizationResult.success(id, sourceFormat, targetFormat, output, duration);

        } catch (Exception e) {
            log.error("Normalization failed: {}", e.getMessage(), e);
            long duration = System.currentTimeMillis() - startTime;
            metrics.recordConversion(LogFormat.TEXT, targetFormat, false, duration);
            return NormalizationResult.failure(id, LogFormat.TEXT, targetFormat, e.getMessage());
        }
    }

    public BatchNormalizationResponse normalizeBatch(BatchNormalizationRequest request) {
        long batchStartTime = System.currentTimeMillis();
        
        List<CompletableFuture<NormalizationResult>> futures = request.getLogs().stream()
                .map(entry -> CompletableFuture.supplyAsync(() -> {
                    if (entry.getFormat() != null) {
                        return normalizeWithKnownFormat(entry, request.getTargetFormat());
                    } else {
                        return normalize(entry.getData(), request.getTargetFormat());
                    }
                }))
                .collect(Collectors.toList());

        List<NormalizationResult> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        // Calculate stats
        Map<String, Integer> formatDistribution = new HashMap<>();
        int successful = 0;
        int failed = 0;

        for (NormalizationResult result : results) {
            if (result.isSuccess()) {
                successful++;
            } else {
                failed++;
            }
            formatDistribution.merge(result.getSourceFormat().name(), 1, Integer::sum);
        }

        BatchNormalizationResponse.BatchStats stats = BatchNormalizationResponse.BatchStats.builder()
                .total(results.size())
                .successful(successful)
                .failed(failed)
                .totalProcessingTimeMs(System.currentTimeMillis() - batchStartTime)
                .formatDistribution(formatDistribution)
                .build();

        return BatchNormalizationResponse.builder()
                .results(results)
                .stats(stats)
                .build();
    }

    private NormalizationResult normalizeWithKnownFormat(
            BatchNormalizationRequest.LogEntry entry, LogFormat targetFormat) {
        long startTime = System.currentTimeMillis();
        String id = entry.getId() != null ? entry.getId() : UUID.randomUUID().toString();

        try {
            LogFormat sourceFormat = entry.getFormat();

            if (sourceFormat == targetFormat) {
                return NormalizationResult.success(id, sourceFormat, targetFormat, 
                        entry.getData(), System.currentTimeMillis() - startTime);
            }

            FormatHandler sourceHandler = handlers.get(sourceFormat);
            FormatHandler targetHandler = handlers.get(targetFormat);

            CanonicalLog canonical = sourceHandler.parse(entry.getData());
            byte[] output = targetHandler.serialize(canonical);

            long duration = System.currentTimeMillis() - startTime;
            metrics.recordConversion(sourceFormat, targetFormat, true, duration);

            return NormalizationResult.success(id, sourceFormat, targetFormat, output, duration);

        } catch (Exception e) {
            return NormalizationResult.failure(id, entry.getFormat(), targetFormat, e.getMessage());
        }
    }

    public NormalizationResult normalizeFallback(byte[] input, LogFormat targetFormat, 
                                                  Throwable t) {
        log.warn("Normalization circuit breaker triggered: {}", t.getMessage());
        return NormalizationResult.failure(
                UUID.randomUUID().toString(),
                LogFormat.TEXT,
                targetFormat,
                "Service temporarily unavailable: " + t.getMessage()
        );
    }

    public Set<LogFormat> getSupportedFormats() {
        return handlers.keySet();
    }
}
