package com.example.logprocessor.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class UdpLogShipperService {
    
    @Value("${udp.server.host:localhost}")
    private String serverHost;
    
    @Value("${udp.server.port:9876}")
    private int serverPort;
    
    @Value("${udp.retry.max:3}")
    private int maxRetries;
    
    @Value("${udp.timeout.seconds:5}")
    private int timeoutSeconds;
    
    private DatagramChannel channel;
    private InetSocketAddress serverAddress;
    private final ObjectMapper objectMapper;
    private final AtomicLong sequenceNumber = new AtomicLong(0);
    private final ConcurrentHashMap<Long, PendingMessage> inFlightMessages = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // Metrics
    private final Counter udpMessagesSent;
    private final Counter udpMessagesRetried;
    private final Counter udpMessagesFailed;
    private final Timer udpSendDuration;
    
    public UdpLogShipperService(ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.udpMessagesSent = Counter.builder("udp.messages.sent")
            .description("Total UDP messages sent")
            .register(meterRegistry);
        this.udpMessagesRetried = Counter.builder("udp.messages.retried")
            .description("Total UDP messages retried")
            .register(meterRegistry);
        this.udpMessagesFailed = Counter.builder("udp.messages.failed")
            .description("Total UDP messages failed")
            .register(meterRegistry);
        this.udpSendDuration = Timer.builder("udp.send.duration")
            .description("Time to send UDP message")
            .register(meterRegistry);
    }
    
    @PostConstruct
    public void init() throws IOException {
        channel = DatagramChannel.open();
        channel.configureBlocking(false);
        serverAddress = new InetSocketAddress(serverHost, serverPort);
        log.info("UDP Log Shipper initialized. Target: {}:{}", serverHost, serverPort);
    }
    
    public CompletableFuture<Void> shipLog(LogEvent event) {
        return udpSendDuration.record(() -> {
            long seq = sequenceNumber.incrementAndGet();
            event.setSequenceNumber(seq);
            event.setTimestamp(Instant.now());
            
            PendingMessage pending = new PendingMessage(event, 0);
            inFlightMessages.put(seq, pending);
            
            return sendWithRetry(pending).whenComplete((result, error) -> {
                inFlightMessages.remove(seq);
                if (error != null) {
                    log.error("Failed to send log event after retries: {}", event.getId(), error);
                    udpMessagesFailed.increment();
                }
            });
        });
    }
    
    private CompletableFuture<Void> sendWithRetry(PendingMessage pending) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            byte[] data = objectMapper.writeValueAsBytes(pending.event);
            ByteBuffer buffer = ByteBuffer.allocate(data.length + 8);
            buffer.putLong(pending.event.getSequenceNumber());
            buffer.put(data);
            buffer.flip();
            
            channel.send(buffer, serverAddress);
            udpMessagesSent.increment();
            
            // Schedule timeout check
            scheduler.schedule(() -> {
                if (!future.isDone() && pending.retries < maxRetries) {
                    pending.retries++;
                    udpMessagesRetried.increment();
                    log.warn("Retrying message seq={} attempt={}", 
                        pending.event.getSequenceNumber(), pending.retries);
                    sendWithRetry(pending).whenComplete((r, e) -> {
                        if (e != null) future.completeExceptionally(e);
                        else future.complete(r);
                    });
                } else if (!future.isDone()) {
                    future.completeExceptionally(
                        new TimeoutException("Max retries exceeded for seq=" + 
                            pending.event.getSequenceNumber()));
                }
            }, timeoutSeconds, TimeUnit.SECONDS);
            
            // Simulate ACK (in real system, would listen for server ACK)
            scheduler.schedule(() -> future.complete(null), 100, TimeUnit.MILLISECONDS);
            
        } catch (IOException e) {
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    @PreDestroy
    public void shutdown() {
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            log.info("UDP Log Shipper shutdown complete");
        } catch (Exception e) {
            log.error("Error during shutdown", e);
        }
    }
    
    private static class PendingMessage {
        LogEvent event;
        int retries;
        
        PendingMessage(LogEvent event, int retries) {
            this.event = event;
            this.retries = retries;
        }
    }
    
    public long getInFlightCount() {
        return inFlightMessages.size();
    }
}
