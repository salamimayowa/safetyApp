package com.nigeria.health.bloodbank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

/**
 * Config: AppConfig
 * Description: Registers shared beans needed by all services.
 *   - RestTemplate: used by SmsService to call Termii API
 *   - @EnableAsync: activates @Async on EmailService and SmsService
 *   - @EnableScheduling: activates @Scheduled on BloodBankScheduler
 * Props: none
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
