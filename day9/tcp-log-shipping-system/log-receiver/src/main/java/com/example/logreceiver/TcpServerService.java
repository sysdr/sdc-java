package com.example.logreceiver;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TcpServerService {

    @Value("${tcp.server.port:9090}")
    private int serverPort;

    @Value("${tcp.server.thread-pool-size:10}")
    private int threadPoolSize;

    @Value("${kafka.topic:logs}")
    private String kafkaTopic;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private volatile boolean running = false;

    // Metrics
    private final Counter logsReceivedCounter;
    private final Counter kafkaPublishCounter;
    private final Counter connectionCounter;

    public TcpServerService(KafkaTemplate<String, String> kafkaTemplate, MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        
        this.logsReceivedCounter = Counter.builder("logs_received_total")
                .description("Total logs received via TCP")
                .register(meterRegistry);
        
        this.kafkaPublishCounter = Counter.builder("kafka_publish_total")
                .description("Total logs published to Kafka")
                .register(meterRegistry);
        
        this.connectionCounter = Counter.builder("tcp_connections_total")
                .description("Total TCP connections accepted")
                .register(meterRegistry);
    }

    @PostConstruct
    public void start() {
        executorService = Executors.newFixedThreadPool(threadPoolSize);
        running = true;

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(serverPort);
                log.info("TCP server started on port {}", serverPort);

                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    connectionCounter.increment();
                    log.info("New client connected: {}", clientSocket.getRemoteSocketAddress());
                    executorService.submit(() -> handleClient(clientSocket));
                }
            } catch (Exception e) {
                if (running) {
                    log.error("Error in TCP server", e);
                }
            }
        }).start();
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logsReceivedCounter.increment();
                
                // Publish to Kafka
                final String logLine = line; // Make it effectively final for lambda
                kafkaTemplate.send(kafkaTopic, logLine)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                kafkaPublishCounter.increment();
                                log.debug("Published log to Kafka: {}", logLine);
                            } else {
                                log.error("Failed to publish to Kafka", ex);
                            }
                        });
            }
        } catch (Exception e) {
            log.error("Error handling client", e);
        } finally {
            try {
                clientSocket.close();
            } catch (Exception e) {
                log.error("Error closing client socket", e);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            executorService.shutdown();
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            log.info("TCP server shutdown complete");
        } catch (Exception e) {
            log.error("Error during shutdown", e);
        }
    }
}
