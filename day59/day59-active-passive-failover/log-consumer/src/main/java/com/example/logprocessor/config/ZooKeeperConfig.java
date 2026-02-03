package com.example.logprocessor.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZooKeeperConfig {
    
    @Value("${zookeeper.connection-string}")
    private String connectionString;
    
    @Bean
    public CuratorFramework curatorFramework() {
        CuratorFramework client = CuratorFrameworkFactory.newClient(
            connectionString,
            new ExponentialBackoffRetry(1000, 3)
        );
        client.start();
        return client;
    }
}
