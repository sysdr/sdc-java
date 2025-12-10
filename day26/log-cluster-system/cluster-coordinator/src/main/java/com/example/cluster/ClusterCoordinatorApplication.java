package com.example.cluster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClusterCoordinatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClusterCoordinatorApplication.class, args);
    }
}
