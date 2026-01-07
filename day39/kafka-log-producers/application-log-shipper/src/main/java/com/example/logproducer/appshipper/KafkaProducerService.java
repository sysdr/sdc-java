package com.example.logproducer.appshipper;

import com.google.common.util.concurrent.RateLimiter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {
    
    private final KafkaTemplate<String, LogEvent> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    
    // Rate limiter: 50,000 events per second
    private final RateLimiter rateLimiter = RateLimiter.create(50000.0);
    
    public void sendLog(LogEvent event) {
        // Apply rate limiting
        if (!rateLimiter.tryAcquire(Duration.ofMillis(100))) {
            Counter.builder("producer.throttled")
                .tag("service", "application-shipper")
                .register(meterRegistry)
                .increment();
            
            throw new RateLimitException("Exceeded 50K events/sec rate limit");
        }
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        CompletableFuture<SendResult<String, LogEvent>> future = 
            kafkaTemplate.send("application-logs", event.getEventId(), event);
        
        future.whenComplete((result, ex) -> {
            sample.stop(Timer.builder("kafka.producer.send.duration")
                .tag("topic", "application-logs")
                .register(meterRegistry));
            
            if (ex == null) {
                Counter.builder("kafka.producer.success")
                    .tag("topic", "application-logs")
                    .register(meterRegistry)
                    .increment();
                
                log.debug("Sent log event {} to partition {} with offset {}",
                    event.getEventId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            } else {
                Counter.builder("kafka.producer.error")
                    .tag("topic", "application-logs")
                    .register(meterRegistry)
                    .increment();
                
                log.error("Failed to send log event {}", event.getEventId(), ex);
            }
        });
    }
}
