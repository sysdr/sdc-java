package com.example.gateway;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogSearchController {

    private final ElasticsearchService elasticsearchService;
    private final RateLimitService rateLimitService;
    private final QueryTranslator queryTranslator;
    private final MeterRegistry meterRegistry;

    @GetMapping("/search")
    @CircuitBreaker(name = "elasticsearch", fallbackMethod = "searchFallback")
    public Mono<ResponseEntity<SearchResponse>> search(
            @Valid SearchRequest request,
            @RequestHeader("X-API-Key") String apiKey) {
        
        log.info("Search request: service={}, level={}, timeRange={}", 
                request.getService(), request.getLevel(), request.getTimeRange());
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        return rateLimitService.checkQuota(apiKey, request.estimateCost())
                .flatMap(allowed -> {
                    if (!allowed) {
                        Counter.builder("api.rate_limit.exceeded")
                                .tag("api_key", apiKey)
                                .register(meterRegistry)
                                .increment();
                        return Mono.error(new RateLimitExceededException("Rate limit exceeded"));
                    }
                    
                    var esQuery = queryTranslator.translate(request);
                    return elasticsearchService.search(esQuery, request.getCursor(), request.getLimit());
                })
                .doOnSuccess(response -> {
                    sample.stop(Timer.builder("api.search.duration")
                            .tag("service", request.getService())
                            .register(meterRegistry));
                    
                    Counter.builder("api.search.success")
                            .tag("service", request.getService())
                            .register(meterRegistry)
                            .increment();
                })
                .map(ResponseEntity::ok);
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<String>> health() {
        return Mono.just(ResponseEntity.ok("OK"));
    }

    private Mono<ResponseEntity<SearchResponse>> searchFallback(
            SearchRequest request, String apiKey, Exception ex) {
        log.error("Circuit breaker activated for search", ex);
        
        Counter.builder("api.circuit_breaker.activated")
                .register(meterRegistry)
                .increment();
        
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new SearchResponse(List.of(), null, 0, 0, true)));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex) {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponse("RATE_LIMIT_EXCEEDED", ex.getMessage()));
    }

    @ExceptionHandler(InvalidQueryException.class)
    public ResponseEntity<ErrorResponse> handleInvalidQuery(InvalidQueryException ex) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("INVALID_QUERY", ex.getMessage()));
    }

    record ErrorResponse(String code, String message) {}
}
