package com.example.facetedsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogConsumerService {

    private final LogDocumentRepository repository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "logs", groupId = "faceted-search-group")
    public void consumeLogEvent(String message) {
        try {
            LogDocument document = objectMapper.readValue(message, LogDocument.class);
            repository.save(document);
            log.debug("Indexed log: {}", document.getId());
        } catch (Exception e) {
            log.error("Failed to index log", e);
        }
    }
}
