package com.nigeria.health.drug;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Drug Authenticity Verification Module — Spring Boot Entry Point
 *
 * Swagger UI: http://localhost:8083/swagger-ui.html
 * API Base:   http://localhost:8083/api/v1
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.nigeria.health.drug",
        "com.nigeria.health.shared"
})
public class DrugVerificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(DrugVerificationApplication.class, args);
    }
}
