package com.example.logprocessor.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class UdpServerService {
    
    @Value("${udp.server.port:9876}")
    private int port;
    
    @Value("${udp.buffer.size:26214400}")
    private int bufferSize;
    
    private DatagramChannel channel;
    private Selector selector;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService executorService;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    // Metrics
    private final Counter packetsReceived;
    private final Counter packetsProcessed;
    private final Counter processingErrors;
    
    public UdpServerService(ObjectMapper objectMapper, 
                           KafkaTemplate<String, String> kafkaTemplate,
                           MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.packetsReceived = Counter.builder("udp.packets.received")
            .description("Total UDP packets received")
            .register(meterRegistry);
        this.packetsProcessed = Counter.builder("udp.packets.processed")
            .description("Total UDP packets processed")
            .register(meterRegistry);
        this.processingErrors = Counter.builder("udp.processing.errors")
            .description("Total processing errors")
            .register(meterRegistry);
    }
    
    @PostConstruct
    public void start() throws IOException {
        channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.socket().setReceiveBufferSize(bufferSize);
        channel.socket().bind(new InetSocketAddress(port));
        
        selector = Selector.open();
        channel.register(selector, SelectionKey.OP_READ);
        
        running.set(true);
        executorService = Executors.newFixedThreadPool(4);
        
        // Start selector loop in separate thread
        new Thread(this::selectorLoop, "udp-selector").start();
        
        log.info("UDP Server started on port {} with buffer size {}", port, bufferSize);
    }
    
    private void selectorLoop() {
        ByteBuffer buffer = ByteBuffer.allocate(65536);
        
        while (running.get()) {
            try {
                if (selector.select(1000) == 0) {
                    continue;
                }
                
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();
                    
                    if (key.isReadable()) {
                        buffer.clear();
                        InetSocketAddress clientAddress = 
                            (InetSocketAddress) channel.receive(buffer);
                        
                        if (clientAddress != null) {
                            packetsReceived.increment();
                            buffer.flip();
                            
                            // Process in thread pool
                            byte[] data = new byte[buffer.remaining()];
                            buffer.get(data);
                            executorService.submit(() -> processPacket(data));
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Error in selector loop", e);
            }
        }
    }
    
    private void processPacket(byte[] data) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            long sequenceNumber = buffer.getLong();
            
            byte[] jsonData = new byte[buffer.remaining()];
            buffer.get(jsonData);
            
            String json = new String(jsonData);
            log.debug("Received packet seq={} data={}", sequenceNumber, json);
            
            // Forward to Kafka
            kafkaTemplate.send("logs.udp.ingress", String.valueOf(sequenceNumber), json)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        packetsProcessed.increment();
                        log.debug("Forwarded to Kafka: seq={}", sequenceNumber);
                    } else {
                        processingErrors.increment();
                        log.error("Failed to forward to Kafka: seq={}", sequenceNumber, ex);
                    }
                });
            
        } catch (Exception e) {
            processingErrors.increment();
            log.error("Error processing packet", e);
        }
    }
    
    @PreDestroy
    public void shutdown() {
        running.set(false);
        try {
            if (executorService != null) {
                executorService.shutdown();
            }
            if (selector != null) {
                selector.close();
            }
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            log.info("UDP Server shutdown complete");
        } catch (Exception e) {
            log.error("Error during shutdown", e);
        }
    }
}
