package com.example.validationgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.validationgateway", "com.example.schemaclient"})
public class ValidationGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ValidationGatewayApplication.class, args);
    }
}
