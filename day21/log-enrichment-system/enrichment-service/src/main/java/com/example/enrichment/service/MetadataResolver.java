package com.example.enrichment.service;

import com.example.enrichment.entity.HostMetadata;
import com.example.enrichment.entity.ServiceMetadata;
import com.example.enrichment.model.EnrichmentMetadata;
import com.example.enrichment.model.LogEvent;
import com.example.enrichment.repository.HostMetadataRepository;
import com.example.enrichment.repository.ServiceMetadataRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class MetadataResolver {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final HostMetadataRepository hostMetadataRepository;
    private final ServiceMetadataRepository serviceMetadataRepository;
    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> fallbackCounters = new HashMap<>();
    
    public MetadataResolver(
            RedisTemplate<String, String> redisTemplate,
            HostMetadataRepository hostMetadataRepository,
            ServiceMetadataRepository serviceMetadataRepository,
            MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.hostMetadataRepository = hostMetadataRepository;
        this.serviceMetadataRepository = serviceMetadataRepository;
        this.meterRegistry = meterRegistry;
    }
    
    public EnrichmentMetadata resolve(LogEvent logEvent) {
        EnrichmentMetadata.EnrichmentMetadataBuilder builder = EnrichmentMetadata.builder();
        Map<String, String> failedLookups = new HashMap<>();
        
        // Resolve hostname from IP
        String hostname = resolveHostname(logEvent.getSourceIp());
        if (hostname == null) {
            failedLookups.put("hostname", "ip_not_found");
        } else {
            builder.hostname(hostname);
            
            // Resolve datacenter from hostname
            String datacenter = resolveDatacenter(hostname);
            if (datacenter == null) {
                failedLookups.put("datacenter", "hostname_not_mapped");
            } else {
                builder.datacenter(datacenter);
            }
        }
        
        // Resolve environment
        String environment = resolveEnvironment();
        builder.environment(environment != null ? environment : "unknown");
        
        // Resolve service metadata
        if (logEvent.getService() != null) {
            ServiceMetadata serviceMeta = resolveServiceMetadata(logEvent.getService());
            if (serviceMeta != null) {
                builder.serviceVersion(serviceMeta.getVersion());
                builder.deploymentId(serviceMeta.getDeploymentId());
                builder.costCenter(serviceMeta.getCostCenter());
            } else {
                failedLookups.put("service_metadata", "service_not_found");
            }
        }
        
        EnrichmentMetadata metadata = builder.build();
        metadata.setFailedLookups(failedLookups);
        
        return metadata;
    }
    
    @CircuitBreaker(name = "redis-hostname", fallbackMethod = "resolveHostnameFallback")
    public String resolveHostname(String ipAddress) {
        if (ipAddress == null) return null;
        
        // Try Redis cache first
        String cached = redisTemplate.opsForValue().get("hostname:" + ipAddress);
        if (cached != null) {
            return cached;
        }
        
        // Fall back to database
        return hostMetadataRepository.findByIpAddress(ipAddress)
            .map(host -> {
                // Cache for 5 minutes
                redisTemplate.opsForValue().set("hostname:" + ipAddress, 
                    host.getHostname(), Duration.ofMinutes(5));
                return host.getHostname();
            })
            .orElse(null);
    }
    
    private String resolveHostnameFallback(String ipAddress, Exception e) {
        log.warn("Circuit breaker open for hostname lookup: {}", e.getMessage());
        incrementFallbackCounter("hostname");
        
        // Direct database lookup without caching
        return hostMetadataRepository.findByIpAddress(ipAddress)
            .map(HostMetadata::getHostname)
            .orElse(null);
    }
    
    @CircuitBreaker(name = "redis-datacenter", fallbackMethod = "resolveDatacenterFallback")
    public String resolveDatacenter(String hostname) {
        if (hostname == null) return null;
        
        // Try Redis cache
        String cached = redisTemplate.opsForValue().get("datacenter:" + hostname);
        if (cached != null) {
            return cached;
        }
        
        // Fall back to database
        return hostMetadataRepository.findByHostname(hostname)
            .map(host -> {
                // Cache for 1 hour (datacenter changes rarely)
                redisTemplate.opsForValue().set("datacenter:" + hostname,
                    host.getDatacenter(), Duration.ofHours(1));
                return host.getDatacenter();
            })
            .orElse(null);
    }
    
    private String resolveDatacenterFallback(String hostname, Exception e) {
        log.warn("Circuit breaker open for datacenter lookup: {}", e.getMessage());
        incrementFallbackCounter("datacenter");
        
        return hostMetadataRepository.findByHostname(hostname)
            .map(HostMetadata::getDatacenter)
            .orElse("unknown");
    }
    
    @CircuitBreaker(name = "redis-environment", fallbackMethod = "resolveEnvironmentFallback")
    public String resolveEnvironment() {
        String env = redisTemplate.opsForValue().get("system:environment");
        return env != null ? env : "production";
    }
    
    private String resolveEnvironmentFallback(Exception e) {
        log.warn("Circuit breaker open for environment lookup: {}", e.getMessage());
        incrementFallbackCounter("environment");
        return "production"; // Safe default
    }
    
    @CircuitBreaker(name = "database-service", fallbackMethod = "resolveServiceMetadataFallback")
    public ServiceMetadata resolveServiceMetadata(String serviceName) {
        if (serviceName == null) return null;
        
        return serviceMetadataRepository.findByServiceName(serviceName)
            .orElse(null);
    }
    
    private ServiceMetadata resolveServiceMetadataFallback(String serviceName, Exception e) {
        log.warn("Circuit breaker open for service metadata lookup: {}", e.getMessage());
        incrementFallbackCounter("service_metadata");
        return null;
    }
    
    private void incrementFallbackCounter(String source) {
        fallbackCounters.computeIfAbsent(source, s ->
            Counter.builder("enrichment.fallback")
                .tag("source", s)
                .description("Circuit breaker fallback invocations")
                .register(meterRegistry)
        ).increment();
    }
}
