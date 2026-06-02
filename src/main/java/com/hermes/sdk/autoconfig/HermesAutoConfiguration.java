package com.hermes.sdk.autoconfig;

import com.hermes.sdk.client.HermesClient;
import com.hermes.sdk.config.HermesProperties;
import com.hermes.sdk.logging.HermesLogger;
import com.hermes.sdk.logging.LogEvents;
import com.hermes.sdk.service.SkillService;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Hermes SDK Spring Boot 自动配置
 * 
 * 引入此 starter 后，自动配置 HermesClient 和 SkillService
 */
@Configuration
@EnableConfigurationProperties(HermesProperties.class)
public class HermesAutoConfiguration {
    
    private static final Logger log = HermesLogger.get(HermesAutoConfiguration.class);
    
    @Bean
    @ConditionalOnMissingBean
    public HermesClient hermesClient(HermesProperties properties) {
        log.info("[{}] baseUrl={}, model={}", 
            LogEvents.CONFIG_INIT, properties.getBaseUrl(), properties.getModel());
        HermesClient client = HermesClient.builder()
            .baseUrl(properties.getBaseUrl())
            .apiKey(properties.getApiKey())
            .model(properties.getModel())
            .build();
        log.info("[{}] HermesClient 初始化完成, baseUrl={}", 
            LogEvents.CONFIG_INIT, properties.getBaseUrl());
        return client;
    }
    
    @Bean
    @ConditionalOnMissingBean
    public SkillService skillService(HermesClient hermesClient) {
        log.info("[{}] SkillService 初始化完成", LogEvents.CONFIG_INIT);
        return new SkillService(hermesClient);
    }
}