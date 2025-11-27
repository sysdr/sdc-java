package com.example.journald.collector;

import com.example.journald.model.JournaldMessage;
import com.example.journald.service.KafkaProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class JournaldCollector {
    
    private static final Logger logger = LoggerFactory.getLogger(JournaldCollector.class);
    private static final String CURSOR_KEY = "journald:cursor";
    
    private final KafkaProducerService kafkaProducer;
    private final StringRedisTemplate redisTemplate;

    public JournaldCollector(KafkaProducerService kafkaProducer, 
                            StringRedisTemplate redisTemplate) {
        this.kafkaProducer = kafkaProducer;
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void collectLogs() {
        try {
            String cursor = redisTemplate.opsForValue().get(CURSOR_KEY);
            String command = buildJournalctlCommand(cursor);
            
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            String line;
            String newCursor = cursor;
            int count = 0;
            
            while ((line = reader.readLine()) != null && count < 1000) {
                JournaldMessage message = parseJournalEntry(line);
                if (message != null) {
                    kafkaProducer.sendJournaldMessage(message);
                    count++;
                }
            }
            
            if (count > 0) {
                logger.info("Collected {} journald entries", count);
            }
            
            reader.close();
            process.waitFor();
            
        } catch (Exception e) {
            logger.error("Error collecting journald logs", e);
        }
    }

    private String buildJournalctlCommand(String cursor) {
        StringBuilder cmd = new StringBuilder("journalctl -o json --no-pager -n 1000");
        if (cursor != null && !cursor.isEmpty()) {
            cmd.append(" --after-cursor=").append(cursor);
        }
        return cmd.toString();
    }

    private JournaldMessage parseJournalEntry(String jsonLine) {
        try {
            // Simulated parsing - in production, use Jackson
            JournaldMessage message = new JournaldMessage();
            message.setTimestamp(Instant.now());
            message.setMessage(jsonLine);
            message.setPriority("INFO");
            message.setHostname("localhost");
            
            Map<String, String> additionalFields = new HashMap<>();
            additionalFields.put("raw", jsonLine);
            message.setAdditionalFields(additionalFields);
            
            return message;
        } catch (Exception e) {
            logger.error("Error parsing journal entry", e);
            return null;
        }
    }
}
