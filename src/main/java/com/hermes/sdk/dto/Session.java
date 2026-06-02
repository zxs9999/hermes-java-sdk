package com.hermes.sdk.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Hermes Session
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Session {
    
    private String sessionId;
    private String title;
    private String mode;
    private Long createdAt;
    private Long updatedAt;
    private String model;
    private String profile;
    private List<Map<String, Object>> summary;
    private Integer messageCount;
}