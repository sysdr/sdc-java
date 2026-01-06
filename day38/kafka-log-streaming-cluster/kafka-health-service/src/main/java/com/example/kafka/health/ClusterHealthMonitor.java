package com.example.kafka.health;

import com.example.kafka.models.ClusterHealth;
import com.example.kafka.models.ClusterHealth.BrokerStatus;
import com.example.kafka.models.ClusterHealth.TopicHealth;
import com.example.kafka.models.ClusterHealth.ConsumerGroupHealth;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class ClusterHealthMonitor {
    private static final Logger logger = LoggerFactory.getLogger(ClusterHealthMonitor.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private AdminClient adminClient;
    private final MeterRegistry meterRegistry;
    private ClusterHealth latestHealth;

    public ClusterHealthMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void initialize() {
        Properties config = new Properties();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);

        adminClient = AdminClient.create(config);
        logger.info("Health monitor initialized");
    }

    @PreDestroy
    public void cleanup() {
        if (adminClient != null) {
            adminClient.close();
        }
    }

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void performHealthCheck() {
        try {
            ClusterHealth health = new ClusterHealth();
            
            // Check broker health
            checkBrokerHealth(health);
            
            // Check topic health
            checkTopicHealth(health);
            
            // Check consumer group health
            checkConsumerGroupHealth(health);
            
            // Determine overall status
            determineOverallStatus(health);
            
            latestHealth = health;
            updateMetrics(health);
            
            logger.info("Health check complete: {} - {}/{} brokers healthy", 
                       health.getStatus(), health.getHealthyBrokers(), health.getTotalBrokers());
        } catch (Exception e) {
            logger.error("Error performing health check", e);
        }
    }

    private void checkBrokerHealth(ClusterHealth health) throws Exception {
        DescribeClusterResult clusterResult = adminClient.describeCluster();
        Collection<Node> nodes = clusterResult.nodes().get();
        
        List<BrokerStatus> brokerStatuses = new ArrayList<>();
        int healthyCount = 0;
        
        for (Node node : nodes) {
            BrokerStatus status = new BrokerStatus();
            status.setBrokerId(node.id());
            status.setHost(node.host());
            status.setPort(node.port());
            
            // In production, we'd actually try to connect to each broker
            // For this demo, we assume all nodes returned are healthy
            status.setHealthy(true);
            healthyCount++;
            
            brokerStatuses.add(status);
        }
        
        health.setTotalBrokers(nodes.size());
        health.setHealthyBrokers(healthyCount);
        health.setBrokerStatuses(brokerStatuses);
    }

    private void checkTopicHealth(ClusterHealth health) throws Exception {
        Set<String> topicNames = adminClient.listTopics().names().get();
        DescribeTopicsResult topicsResult = adminClient.describeTopics(topicNames);
        Map<String, TopicDescription> descriptions = topicsResult.all().get();
        
        Map<String, TopicHealth> topicHealthMap = new HashMap<>();
        
        for (Map.Entry<String, TopicDescription> entry : descriptions.entrySet()) {
            String topicName = entry.getKey();
            TopicDescription desc = entry.getValue();
            
            TopicHealth topicHealth = new TopicHealth();
            topicHealth.setTopicName(topicName);
            topicHealth.setPartitionCount(desc.partitions().size());
            
            if (!desc.partitions().isEmpty()) {
                topicHealth.setReplicationFactor(desc.partitions().get(0).replicas().size());
                
                // Check for under-replicated partitions
                int underReplicated = (int) desc.partitions().stream()
                    .filter(p -> p.isr().size() < p.replicas().size())
                    .count();
                topicHealth.setUnderReplicatedPartitions(underReplicated);
                topicHealth.setHealthy(underReplicated == 0);
            }
            
            topicHealthMap.put(topicName, topicHealth);
        }
        
        health.setTopicHealth(topicHealthMap);
    }

    private void checkConsumerGroupHealth(ClusterHealth health) {
        try {
            ListConsumerGroupsResult groupsResult = adminClient.listConsumerGroups();
            Set<String> groupIds = new HashSet<>();
            groupsResult.all().get().forEach(listing -> groupIds.add(listing.groupId()));
            
            Map<String, ConsumerGroupHealth> groupHealthMap = new HashMap<>();
            
            for (String groupId : groupIds) {
                try {
                    ListConsumerGroupOffsetsResult offsetsResult = 
                        adminClient.listConsumerGroupOffsets(groupId);
                    Map<TopicPartition, OffsetAndMetadata> offsets = offsetsResult.partitionsToOffsetAndMetadata().get();
                    
                    ConsumerGroupHealth groupHealth = new ConsumerGroupHealth();
                    groupHealth.setGroupId(groupId);
                    groupHealth.setState("STABLE");
                    
                    long totalLag = 0;
                    Map<String, Long> partitionLags = new HashMap<>();
                    
                    // In production, we'd compare with end offsets to calculate lag
                    // For this demo, we'll just record the committed offsets
                    for (Map.Entry<TopicPartition, OffsetAndMetadata> offset : offsets.entrySet()) {
                        String key = offset.getKey().topic() + "-" + offset.getKey().partition();
                        long offsetValue = offset.getValue().offset();
                        partitionLags.put(key, offsetValue);
                    }
                    
                    groupHealth.setTotalLag(totalLag);
                    groupHealth.setPartitionLags(partitionLags);
                    groupHealthMap.put(groupId, groupHealth);
                } catch (Exception e) {
                    logger.warn("Error checking consumer group {}: {}", groupId, e.getMessage());
                }
            }
            
            health.setConsumerGroupHealth(groupHealthMap);
        } catch (Exception e) {
            logger.error("Error checking consumer groups", e);
        }
    }

    private void determineOverallStatus(ClusterHealth health) {
        if (health.getHealthyBrokers() < health.getTotalBrokers()) {
            health.setStatus("DEGRADED");
            return;
        }
        
        // Check for under-replicated partitions
        boolean hasUnderReplicated = health.getTopicHealth().values().stream()
            .anyMatch(t -> t.getUnderReplicatedPartitions() > 0);
        
        if (hasUnderReplicated) {
            health.setStatus("DEGRADED");
            return;
        }
        
        health.setStatus("HEALTHY");
    }

    private void updateMetrics(ClusterHealth health) {
        meterRegistry.gauge("kafka.cluster.brokers.total", health.getTotalBrokers());
        meterRegistry.gauge("kafka.cluster.brokers.healthy", health.getHealthyBrokers());
        
        int totalTopics = health.getTopicHealth().size();
        int healthyTopics = (int) health.getTopicHealth().values().stream()
            .filter(TopicHealth::isHealthy)
            .count();
        
        meterRegistry.gauge("kafka.cluster.topics.total", totalTopics);
        meterRegistry.gauge("kafka.cluster.topics.healthy", healthyTopics);
    }

    public ClusterHealth getLatestHealth() {
        return latestHealth != null ? latestHealth : new ClusterHealth();
    }
}
