package com.example.partition.service;

import com.example.partition.entity.LogEntryEntity;
import com.example.partition.repository.LogEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartitionQueryService {
    
    private final LogEntryRepository repository;
    
    public Flux<LogEntryEntity> executeQuery(
            String logLevel,
            String serviceName,
            Instant startTime,
            Instant endTime,
            Integer limit) {
        
        return Flux.defer(() -> {
            Instant start = startTime != null ? startTime : Instant.now().minusSeconds(3600);
            Instant end = endTime != null ? endTime : Instant.now();
            
            log.info("Executing partition query: level={}, service={}, range=[{} - {}]",
                logLevel, serviceName, start, end);
            
            List<LogEntryEntity> results = repository.findByFilters(
                logLevel, serviceName, start, end
            );
            
            log.info("Query returned {} results", results.size());
            
            int maxResults = limit != null ? limit : 1000;
            return Flux.fromIterable(results)
                .take(maxResults);
        });
    }
}
