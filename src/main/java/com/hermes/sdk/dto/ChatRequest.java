package com.hermes.sdk.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Chat 请求 DTO
 */
@Data
public class ChatRequest {
    
    @NotBlank(message = "消息内容不能为空")
    private String message;
    
    /** 可选：System Prompt */
    private String systemPrompt;
}