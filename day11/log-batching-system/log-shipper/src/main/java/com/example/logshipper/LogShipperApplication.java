package com.example.logshipper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LogShipperApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogShipperApplication.class, args);
    }
}
