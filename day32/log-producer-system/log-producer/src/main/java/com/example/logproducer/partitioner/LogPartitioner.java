package com.example.logproducer.partitioner;

import com.example.logproducer.model.LogEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Custom partitioner for log events
 * - ERROR/FATAL logs go to partition 0 for priority processing
 * - Other logs are distributed based on source hash for even distribution
 */
public class LogPartitioner implements Partitioner {
    
    private static final int PRIORITY_PARTITION = 0;
    
    @Override
    public int partition(String topic, Object key, byte[] keyBytes, 
                        Object value, byte[] valueBytes, Cluster cluster) {
        
        List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
        int numPartitions = partitions.size();
        
        if (numPartitions == 0) {
            return 0;
        }
        
        try {
            // Parse the log event to check severity
            ObjectMapper mapper = new ObjectMapper();
            LogEvent logEvent = mapper.readValue(valueBytes, LogEvent.class);
            
            // ERROR and FATAL logs go to priority partition
            if (logEvent.getLevel() == LogEvent.LogLevel.ERROR || 
                logEvent.getLevel() == LogEvent.LogLevel.FATAL) {
                return PRIORITY_PARTITION % numPartitions;
            }
            
            // Distribute other logs based on source hash
            String source = logEvent.getSource();
            return Math.abs(source.hashCode()) % numPartitions;
            
        } catch (Exception e) {
            // Fallback to key-based partitioning
            if (key != null) {
                return Math.abs(key.hashCode()) % numPartitions;
            }
            return 0;
        }
    }
    
    @Override
    public void close() {
        // Cleanup if needed
    }
    
    @Override
    public void configure(Map<String, ?> configs) {
        // Configuration if needed
    }
}
