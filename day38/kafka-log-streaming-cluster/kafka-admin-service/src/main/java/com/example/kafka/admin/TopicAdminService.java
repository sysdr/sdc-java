package com.example.kafka.admin;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Service
public class TopicAdminService {
    private static final Logger logger = LoggerFactory.getLogger(TopicAdminService.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private AdminClient adminClient;

    @PostConstruct
    public void initialize() {
        Properties config = new Properties();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 60000);

        adminClient = AdminClient.create(config);
        logger.info("Kafka AdminClient initialized with bootstrap servers: {}", bootstrapServers);

        // Create default topics
        createDefaultTopics();
    }

    @PreDestroy
    public void cleanup() {
        if (adminClient != null) {
            adminClient.close();
            logger.info("Kafka AdminClient closed");
        }
    }

    private void createDefaultTopics() {
        try {
            List<NewTopic> topics = List.of(
                new NewTopic("log-events", 12, (short) 3)
                    .configs(Map.of(
                        "retention.ms", "604800000", // 7 days
                        "compression.type", "snappy",
                        "min.insync.replicas", "2"
                    )),
                new NewTopic("critical-logs", 4, (short) 3)
                    .configs(Map.of(
                        "retention.ms", "1209600000", // 14 days
                        "compression.type", "snappy",
                        "min.insync.replicas", "2"
                    )),
                new NewTopic("audit-logs", 6, (short) 3)
                    .configs(Map.of(
                        "retention.ms", "2592000000", // 30 days
                        "compression.type", "snappy",
                        "min.insync.replicas", "2",
                        "cleanup.policy", "compact"
                    ))
            );

            CreateTopicsResult result = adminClient.createTopics(topics);
            result.all().get();
            logger.info("Successfully created default topics: log-events, critical-logs, audit-logs");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof org.apache.kafka.common.errors.TopicExistsException) {
                logger.info("Topics already exist, skipping creation");
            } else {
                logger.error("Error creating topics", e);
            }
        } catch (Exception e) {
            logger.error("Error creating topics", e);
        }
    }

    public void createTopic(String topicName, int partitions, short replicationFactor, 
                           Map<String, String> configs) throws Exception {
        NewTopic newTopic = new NewTopic(topicName, partitions, replicationFactor);
        if (configs != null && !configs.isEmpty()) {
            newTopic.configs(configs);
        }

        CreateTopicsResult result = adminClient.createTopics(Collections.singleton(newTopic));
        result.all().get();
        logger.info("Created topic: {} with {} partitions and replication factor {}", 
                   topicName, partitions, replicationFactor);
    }

    public void deleteTopic(String topicName) throws Exception {
        DeleteTopicsResult result = adminClient.deleteTopics(Collections.singleton(topicName));
        result.all().get();
        logger.info("Deleted topic: {}", topicName);
    }

    public Set<String> listTopics() throws Exception {
        ListTopicsResult result = adminClient.listTopics();
        return result.names().get();
    }

    public Map<String, TopicDescription> describeTopics(Set<String> topicNames) throws Exception {
        DescribeTopicsResult result = adminClient.describeTopics(topicNames);
        return result.all().get();
    }

    public TopicDescription describeTopic(String topicName) throws Exception {
        Map<String, TopicDescription> descriptions = describeTopics(Collections.singleton(topicName));
        return descriptions.get(topicName);
    }
}
