package com.logprocessor.repair;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ReadRepairApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReadRepairApplication.class, args);
    }
}
