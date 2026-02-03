package com.example.logprocessor.producer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.clients.admin.NewTopic;

/**
 * Kafka topic auto-creation configuration.
 *
 * Each region gets its own log topic. The naming convention
 * (log-events-{region}) is critical: MirrorMaker 2 uses a regex
 * filter on this prefix to decide which topics to replicate.
 *
 * Replication factor = 2 within each region for broker-level HA.
 * Partitions = 8 to allow parallel consumer scaling within a region.
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${app.region}")
    private String region;

    @Bean
    public NewTopic logEventsTopic() {
        return TopicBuilder.name("log-events-" + region)
                .partitions(8)
                .replicas(1)  // Single-broker dev clusters; set to 2+ in prod
                .build();
    }

    @Bean
    public NewTopic deadLetterTopic() {
        return TopicBuilder.name("log-events-" + region + "-dlq")
                .partitions(2)
                .replicas(1)
                .build();
    }
}
