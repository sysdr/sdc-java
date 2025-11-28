package com.example.partitionmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PartitionManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(PartitionManagerApplication.class, args);
    }
}
