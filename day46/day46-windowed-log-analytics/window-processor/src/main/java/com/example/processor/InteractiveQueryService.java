package com.example.processor;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class InteractiveQueryService {
    private static final Logger logger = LoggerFactory.getLogger(InteractiveQueryService.class);
    
    private StreamsBuilderFactoryBean factoryBean;
    
    @Autowired(required = false)
    public void setFactoryBean(StreamsBuilderFactoryBean factoryBean) {
        this.factoryBean = factoryBean;
    }
    
    public List<WindowStats> queryTumblingWindow(String key, Instant from, Instant to) {
        return queryWindowStore("tumbling-windows-store", key, from, to);
    }
    
    public List<WindowStats> queryHoppingWindow(String key, Instant from, Instant to) {
        return queryWindowStore("hopping-windows-store", key, from, to);
    }
    
    private List<WindowStats> queryWindowStore(String storeName, String key, Instant from, Instant to) {
        List<WindowStats> results = new ArrayList<>();
        
        if (factoryBean == null) {
            logger.warn("StreamsBuilderFactoryBean not available for queries");
            return results;
        }
        
        try {
            KafkaStreams streams = factoryBean.getKafkaStreams();
            if (streams == null || !streams.state().isRunningOrRebalancing()) {
                logger.warn("Kafka Streams not ready for queries");
                return results;
            }
            
            ReadOnlyWindowStore<String, WindowStats> store = streams.store(
                StoreQueryParameters.fromNameAndType(storeName, QueryableStoreTypes.windowStore())
            );
            
            WindowStoreIterator<WindowStats> iterator = store.fetch(key, from, to);
            
            while (iterator.hasNext()) {
                results.add(iterator.next().value);
            }
            
            iterator.close();
            
        } catch (Exception e) {
            logger.error("Failed to query window store {}: {}", storeName, e.getMessage());
        }
        
        return results;
    }
}
