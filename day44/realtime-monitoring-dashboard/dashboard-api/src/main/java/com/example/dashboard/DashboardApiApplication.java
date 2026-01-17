package com.example.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DashboardApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(DashboardApiApplication.class, args);
    }
}
