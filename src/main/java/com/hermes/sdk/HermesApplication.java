package com.hermes.sdk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Hermes SDK Spring Boot 示例应用（Spring Boot 2.x）
 */
@SpringBootApplication
@EnableConfigurationProperties(com.hermes.sdk.config.HermesProperties.class)
public class HermesApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(HermesApplication.class, args);
    }
}