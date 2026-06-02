package com.hermes.sdk.controller;

import com.hermes.sdk.client.HermesClient;
import com.hermes.sdk.dto.ChatRequest;
import com.hermes.sdk.dto.SkillRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * Hermes SDK REST Controller
 * 
 * 提供 HTTP API 给第三方调用
 */
@Slf4j
@RestController
@RequestMapping("/api/hermes")
@RequiredArgsConstructor
@Validated
public class HermesController {
    
    private final HermesClient hermesClient;
    
    /**
     * 简单聊天
     * POST /api/hermes/chat
     */
    @PostMapping("/chat")
    public ResponseEntity<String> chat(@Valid @RequestBody ChatRequest request) {
        log.info("收到聊天请求: {}", request.getMessage());
        String response = hermesClient.chat(request.getMessage());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 激活 Skill
     * POST /api/hermes/skill
     */
    @PostMapping("/skill")
    public ResponseEntity<String> activateSkill(@Valid @RequestBody SkillRequest request) {
        log.info("激活 Skill: {}", request.getSkillName());
        String response = hermesClient.activateSkill(request.getSkillName(), request.getTask());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 带 System Prompt 的聊天
     * POST /api/hermes/chat/system
     */
    @PostMapping("/chat/system")
    public ResponseEntity<String> chatWithSystem(@Valid @RequestBody ChatRequest request) {
        log.info("收到带 System Prompt 的聊天请求");
        String response = hermesClient.chatWithSystemPrompt(request.getSystemPrompt(), request.getMessage());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 健康检查
     * GET /api/hermes/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Hermes SDK OK");
    }
}