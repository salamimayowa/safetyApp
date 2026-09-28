package com.nigeria.health.accident;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Road Accident Reporting Module — Spring Boot Entry Point
 *
 * Swagger UI: http://localhost:8082/swagger-ui.html
 * API Base:   http://localhost:8082/api/v1
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.nigeria.health.accident",
        "com.nigeria.health.shared"
})
public class AccidentReportApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccidentReportApplication.class, args);
    }
}
