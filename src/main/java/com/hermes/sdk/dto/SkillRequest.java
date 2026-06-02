package com.hermes.sdk.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Skill 激活请求 DTO
 */
@Data
public class SkillRequest {
    
    @NotBlank(message = "Skill 名称不能为空")
    private String skillName;
    
    @NotBlank(message = "任务描述不能为空")
    private String task;
}