package com.hermes.sdk.autoconfig;

import com.hermes.sdk.client.HermesClient;
import com.hermes.sdk.config.HermesProperties;
import com.hermes.sdk.service.SkillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Hermes SDK Spring Boot 自动配置
 * 
 * 引入此 starter 后，自动配置 HermesClient 和 SkillService
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(HermesProperties.class)
public class HermesAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public HermesClient hermesClient(HermesProperties properties) {
        log.info("初始化 HermesClient: {}", properties.getBaseUrl());
        return new HermesClient(properties.getBaseUrl(), properties.getApiKey());
    }
    
    @Bean
    @ConditionalOnMissingBean
    public SkillService skillService(HermesClient hermesClient) {
        return new SkillService(hermesClient);
    }
}