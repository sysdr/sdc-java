package com.logprocessor.merkle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MerkleTreeApplication {
    public static void main(String[] args) {
        SpringApplication.run(MerkleTreeApplication.class, args);
    }
}
