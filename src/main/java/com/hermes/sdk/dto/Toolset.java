package com.hermes.sdk.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

/**
 * Toolset 元信息
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Toolset {
    private String name;
    private String label;
    private String description;
    private boolean enabled;
    private boolean configured;
    private List<String> tools;
}

/**
 * Toolsets 列表响应
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class ToolsetsResponse {
    private String object;
    private String platform;
    private List<Toolset> data;
}