package com.example.journald;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JournaldAdapterApplication {
    public static void main(String[] args) {
        SpringApplication.run(JournaldAdapterApplication.class, args);
    }
}
