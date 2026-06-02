package com.hermes.sdk.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Skill 元信息
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Skill {
    private String name;
    private String description;
    private String category;
    private boolean enabled;
}

/**
 * Skills 列表响应
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class SkillsResponse {
    private String object;
    private List<Skill> data;
}