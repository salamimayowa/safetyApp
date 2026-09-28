package com.nigeria.health.bloodbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Blood Bank Network Module — Spring Boot Entry Point
 *
 * Scans both this module and the shared module for components.
 * Run this class to start the Blood Bank API on port 8081.
 *
 * Swagger UI: http://localhost:8081/swagger-ui.html
 * API Base:   http://localhost:8081/api/v1
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.nigeria.health.bloodbank",
        "com.nigeria.health.shared"
})
public class BloodBankApplication {
    public static void main(String[] args) {
        SpringApplication.run(BloodBankApplication.class, args);
    }
}
