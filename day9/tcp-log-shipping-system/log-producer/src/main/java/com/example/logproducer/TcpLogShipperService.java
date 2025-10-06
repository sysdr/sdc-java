package com.example.logproducer;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class TcpLogShipperService {

    @Value("${log.shipper.host:localhost}")
    private String serverHost;

    @Value("${log.shipper.port:9090}")
    private int serverPort;

    @Value("${log.shipper.batch-size:100}")
    private int batchSize;

    @Value("${log.shipper.buffer-capacity:10000}")
    private int bufferCapacity;

    private BlockingQueue<String> logBuffer;
    private Socket socket;
    private BufferedWriter writer;
    private CircuitBreaker circuitBreaker;
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private volatile boolean running = true;

    // Metrics
    private final Counter logsSentCounter;
    private final Counter logsDroppedCounter;
    private final Counter connectionFailuresCounter;
    private final AtomicInteger bufferDepthGauge = new AtomicInteger(0);

    public TcpLogShipperService(MeterRegistry meterRegistry) {
        
        // Initialize metrics
        this.logsSentCounter = Counter.builder("logs_sent_total")
                .description("Total number of logs sent to receiver")
                .register(meterRegistry);
        
        this.logsDroppedCounter = Counter.builder("logs_dropped_total")
                .description("Total number of logs dropped")
                .register(meterRegistry);
        
        this.connectionFailuresCounter = Counter.builder("connection_failures_total")
                .description("Total number of connection failures")
                .register(meterRegistry);
        
        Gauge.builder("buffer_depth", bufferDepthGauge, AtomicInteger::get)
                .description("Current depth of log buffer")
                .register(meterRegistry);
    }

    @PostConstruct
    public void init() {
        // Initialize log buffer
        this.logBuffer = new LinkedBlockingQueue<>(bufferCapacity);
        
        // Configure circuit breaker
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .build();
        
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        this.circuitBreaker = registry.circuitBreaker("logShipper");
        
        // Initial connection attempt
        connectToServer();
    }

    private void connectToServer() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            
            socket = new Socket(serverHost, serverPort);
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            reconnectAttempts.set(0);
            log.info("Connected to log receiver at {}:{}", serverHost, serverPort);
        } catch (Exception e) {
            connectionFailuresCounter.increment();
            int attempt = reconnectAttempts.incrementAndGet();
            long backoffMs = Math.min(1000 * (long) Math.pow(2, attempt), 30000);
            log.error("Failed to connect to server (attempt {}). Retrying in {}ms", attempt, backoffMs, e);
        }
    }

    public boolean shipLog(String logMessage) {
        boolean offered = logBuffer.offer(logMessage);
        if (!offered) {
            logsDroppedCounter.increment();
            log.warn("Buffer full, dropping log message");
        }
        bufferDepthGauge.set(logBuffer.size());
        return offered;
    }

    @Scheduled(fixedRate = 100) // Flush every 100ms
    public void flushBuffer() {
        if (logBuffer.isEmpty()) {
            return;
        }

        List<String> batch = new ArrayList<>();
        logBuffer.drainTo(batch, batchSize);
        bufferDepthGauge.set(logBuffer.size());

        if (batch.isEmpty()) {
            return;
        }

        try {
            circuitBreaker.executeRunnable(() -> sendBatch(batch));
        } catch (Exception e) {
            // Circuit breaker is open or transmission failed
            log.warn("Failed to send batch, re-queuing messages: {}", e.getMessage());
            // Re-queue messages (may drop if buffer is full)
            batch.forEach(this::shipLog);
        }
    }

    private void sendBatch(List<String> batch) {
        try {
            if (socket == null || socket.isClosed() || !socket.isConnected()) {
                connectToServer();
            }

            for (String log : batch) {
                writer.write(log);
                writer.newLine();
            }
            writer.flush();
            
            logsSentCounter.increment(batch.size());
            log.debug("Sent batch of {} logs", batch.size());
        } catch (Exception e) {
            connectionFailuresCounter.increment();
            log.error("Error sending batch", e);
            throw new RuntimeException("Failed to send batch", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        try {
            // Flush remaining logs
            if (!logBuffer.isEmpty()) {
                List<String> remaining = new ArrayList<>();
                logBuffer.drainTo(remaining);
                sendBatch(remaining);
            }
            
            if (writer != null) {
                writer.close();
            }
            if (socket != null) {
                socket.close();
            }
            log.info("TCP log shipper shutdown complete");
        } catch (Exception e) {
            log.error("Error during shutdown", e);
        }
    }
}
