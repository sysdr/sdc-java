package com.example.logproducer.infrashipper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InfrastructureLogShipperApplication {
    public static void main(String[] args) {
        SpringApplication.run(InfrastructureLogShipperApplication.class, args);
    }
}
