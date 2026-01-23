package com.example.mapreduce.mapper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class MapWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MapWorkerApplication.class, args);
    }
}
