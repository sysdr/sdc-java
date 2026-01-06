package com.example.kafka.models;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ClusterHealth {
    private String status; // HEALTHY, DEGRADED, CRITICAL
    private Instant checkTime;
    private int totalBrokers;
    private int healthyBrokers;
    private List<BrokerStatus> brokerStatuses;
    private Map<String, TopicHealth> topicHealth;
    private Map<String, ConsumerGroupHealth> consumerGroupHealth;

    public ClusterHealth() {
        this.checkTime = Instant.now();
    }

    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCheckTime() { return checkTime; }
    public void setCheckTime(Instant checkTime) { this.checkTime = checkTime; }

    public int getTotalBrokers() { return totalBrokers; }
    public void setTotalBrokers(int totalBrokers) { this.totalBrokers = totalBrokers; }

    public int getHealthyBrokers() { return healthyBrokers; }
    public void setHealthyBrokers(int healthyBrokers) { this.healthyBrokers = healthyBrokers; }

    public List<BrokerStatus> getBrokerStatuses() { return brokerStatuses; }
    public void setBrokerStatuses(List<BrokerStatus> brokerStatuses) { 
        this.brokerStatuses = brokerStatuses; 
    }

    public Map<String, TopicHealth> getTopicHealth() { return topicHealth; }
    public void setTopicHealth(Map<String, TopicHealth> topicHealth) { 
        this.topicHealth = topicHealth; 
    }

    public Map<String, ConsumerGroupHealth> getConsumerGroupHealth() { 
        return consumerGroupHealth; 
    }
    public void setConsumerGroupHealth(Map<String, ConsumerGroupHealth> consumerGroupHealth) { 
        this.consumerGroupHealth = consumerGroupHealth; 
    }

    public static class BrokerStatus {
        private int brokerId;
        private String host;
        private int port;
        private boolean isHealthy;
        private String errorMessage;

        // Getters and Setters
        public int getBrokerId() { return brokerId; }
        public void setBrokerId(int brokerId) { this.brokerId = brokerId; }

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }

        public boolean isHealthy() { return isHealthy; }
        public void setHealthy(boolean healthy) { isHealthy = healthy; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    public static class TopicHealth {
        private String topicName;
        private int partitionCount;
        private int replicationFactor;
        private int underReplicatedPartitions;
        private boolean isHealthy;

        // Getters and Setters
        public String getTopicName() { return topicName; }
        public void setTopicName(String topicName) { this.topicName = topicName; }

        public int getPartitionCount() { return partitionCount; }
        public void setPartitionCount(int partitionCount) { this.partitionCount = partitionCount; }

        public int getReplicationFactor() { return replicationFactor; }
        public void setReplicationFactor(int replicationFactor) { 
            this.replicationFactor = replicationFactor; 
        }

        public int getUnderReplicatedPartitions() { return underReplicatedPartitions; }
        public void setUnderReplicatedPartitions(int underReplicatedPartitions) { 
            this.underReplicatedPartitions = underReplicatedPartitions; 
        }

        public boolean isHealthy() { return isHealthy; }
        public void setHealthy(boolean healthy) { isHealthy = healthy; }
    }

    public static class ConsumerGroupHealth {
        private String groupId;
        private String state;
        private long totalLag;
        private Map<String, Long> partitionLags;

        // Getters and Setters
        public String getGroupId() { return groupId; }
        public void setGroupId(String groupId) { this.groupId = groupId; }

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }

        public long getTotalLag() { return totalLag; }
        public void setTotalLag(long totalLag) { this.totalLag = totalLag; }

        public Map<String, Long> getPartitionLags() { return partitionLags; }
        public void setPartitionLags(Map<String, Long> partitionLags) { 
            this.partitionLags = partitionLags; 
        }
    }
}
