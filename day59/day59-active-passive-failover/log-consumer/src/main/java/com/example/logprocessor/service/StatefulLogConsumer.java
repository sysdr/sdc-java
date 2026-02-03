package com.example.logprocessor.service;

import com.example.logprocessor.model.ConsumerState;
import com.example.logprocessor.model.LogEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class StatefulLogConsumer {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private final StateManager stateManager;
    private final Counter messagesProcessedCounter;
    private final ObjectMapper objectMapper;
    
    private final AtomicBoolean isActive = new AtomicBoolean(false);
    private final AtomicLong currentEpoch = new AtomicLong(0);
    private final AtomicLong messagesProcessed = new AtomicLong(0);
    
    private String instanceId;
    
    public StatefulLogConsumer(
            StateManager stateManager,
            Counter messagesProcessedCounter) {
        this.stateManager = stateManager;
        this.messagesProcessedCounter = messagesProcessedCounter;
        this.objectMapper = new ObjectMapper();
    }
    
    @KafkaListener(
        topics = "${kafka.topic.log-events}",
        groupId = "${kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeLogEvent(
            ConsumerRecord<String, String> record,
            Acknowledgment acknowledgment) {
        
        // Only process if this instance is the active leader
        if (!isActive.get()) {
            log.debug("Skipping message, instance is standby");
            return;
        }
        
        try {
            JsonNode jsonNode = objectMapper.readTree(record.value());
            String messageId = jsonNode.get("messageId").asText();
            int partition = record.partition();
            
            // Check for duplicate processing
            if (stateManager.isMessageProcessed(messageId, partition)) {
                log.debug("Message {} already processed, skipping", messageId);
                acknowledgment.acknowledge();
                return;
            }
            
            // Process the message
            LogEvent logEvent = LogEvent.builder()
                .messageId(messageId)
                .level(jsonNode.get("level").asText())
                .message(jsonNode.get("message").asText())
                .source(jsonNode.get("source").asText())
                .timestamp(Instant.ofEpochMilli(jsonNode.get("timestamp").asLong()))
                .processedAt(Instant.now())
                .processorEpoch(currentEpoch.get())
                .processorInstanceId(instanceId)
                .build();
            
            entityManager.persist(logEvent);
            
            // Mark as processed (idempotency)
            stateManager.markMessageProcessed(messageId, partition);
            
            // Update state snapshot
            stateManager.updatePartitionOffset(partition, record.offset());
            
            // Update metrics
            long processed = messagesProcessed.incrementAndGet();
            messagesProcessedCounter.increment();
            
            if (processed % 100 == 0) {
                log.info("Processed {} messages as leader (epoch: {})", processed, currentEpoch.get());
            }
            
            // Commit offset
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing message from partition {}", record.partition(), e);
            // Acknowledge to prevent reprocessing of bad messages
            // In production, send to DLQ instead
            try {
                acknowledgment.acknowledge();
            } catch (Exception ackEx) {
                log.error("Failed to acknowledge message", ackEx);
            }
        }
    }
    
    public void resumeAsLeader(long epoch) {
        log.info("Resuming consumption as LEADER with epoch {}", epoch);
        
        // Load previous state
        ConsumerState state = stateManager.loadState();
        if (state != null && state.getPartitionOffsets() != null) {
            log.info("Resuming from offsets: {}", state.getPartitionOffsets());
            messagesProcessed.set(state.getTotalMessagesProcessed());
        }
        
        this.currentEpoch.set(epoch);
        this.isActive.set(true);
        
        log.info("✅ Ready to process messages");
    }
    
    public void pauseAsStandby() {
        log.info("Pausing consumption as STANDBY");
        
        // Save final state
        ConsumerState state = ConsumerState.builder()
            .instanceId(instanceId)
            .isLeader(false)
            .epoch(currentEpoch.get())
            .totalMessagesProcessed(messagesProcessed.get())
            .lastHeartbeat(Instant.now())
            .stateTimestamp(Instant.now())
            .build();
        stateManager.saveState(state);
        
        this.isActive.set(false);
        
        log.info("Standby mode activated");
    }
    
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
    
    public boolean isActive() {
        return isActive.get();
    }
    
    public long getMessagesProcessed() {
        return messagesProcessed.get();
    }
}
