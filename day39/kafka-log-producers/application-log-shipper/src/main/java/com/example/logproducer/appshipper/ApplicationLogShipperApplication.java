package com.example.logproducer.appshipper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApplicationLogShipperApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApplicationLogShipperApplication.class, args);
    }
}
