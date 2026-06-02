package com.hermes.sdk.controller;

import com.hermes.sdk.client.HermesClient;
import com.hermes.sdk.dto.ChatRequest;
import com.hermes.sdk.dto.SkillRequest;
import com.hermes.sdk.exception.HermesApiException;
import com.hermes.sdk.exception.HermesAuthException;
import com.hermes.sdk.exception.HermesException;
import com.hermes.sdk.exception.HermesNetworkException;
import com.hermes.sdk.logging.HermesLogger;
import com.hermes.sdk.logging.LogEvents;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Hermes REST API 控制器
 * 
 * 统一的异常处理 + 健康检查
 */
public class HermesController {
    
    private static final Logger log = HermesLogger.get(HermesController.class);
    
    private final HermesClient hermesClient;
    
    public HermesController(HermesClient hermesClient) {
        this.hermesClient = hermesClient;
    }
    
    /**
     * 聊天
     */
    @PostMapping("/api/hermes/chat")
    public ResponseEntity<Map<String, Object>> chat(@Valid ChatRequest request) {
        try {
            log.info("[{}] message={}", LogEvents.CHAT_REQUEST, maskContent(request.getMessage()));
            String result = hermesClient.chat(request.getMessage());
            log.info("[{}] success, responseLength={}", LogEvents.CHAT_RESPONSE, result.length());
            return success(result);
        } catch (HermesAuthException e) {
            log.warn("[{}] AUTH_ERROR: {}", LogEvents.CHAT_ERROR, e.getMessage());
            return error("AUTH_ERROR", e.getMessage(), HttpStatus.UNAUTHORIZED);
        } catch (HermesNetworkException e) {
            log.warn("[{}] NETWORK_ERROR: {}", LogEvents.CHAT_ERROR, e.getMessage());
            return error("NETWORK_ERROR", e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (HermesException e) {
            log.warn("[{}] {}: {}", LogEvents.CHAT_ERROR, e.getErrorCode(), e.getMessage());
            return error(e.getErrorCode(), e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("[{}] UNKNOWN: {}", LogEvents.CHAT_ERROR, e.getMessage(), e);
            return error("UNKNOWN", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 激活 Skill
     */
    @PostMapping("/api/hermes/skill")
    public ResponseEntity<Map<String, Object>> skill(@Valid SkillRequest request) {
        try {
            log.info("[{}] skillName={}, task={}", 
                LogEvents.SKILL_ACTIVATE, request.getSkillName(), maskContent(request.getTask()));
            String result = hermesClient.activateSkill(request.getSkillName(), request.getTask());
            log.info("[{}] success, responseLength={}", LogEvents.SKILL_ACTIVATE, result.length());
            return success(result);
        } catch (HermesAuthException e) {
            log.warn("[{}] AUTH_ERROR: {}", LogEvents.SKILL_ACTIVATE, e.getMessage());
            return error("AUTH_ERROR", e.getMessage(), HttpStatus.UNAUTHORIZED);
        } catch (HermesNetworkException e) {
            log.warn("[{}] NETWORK_ERROR: {}", LogEvents.SKILL_ACTIVATE, e.getMessage());
            return error("NETWORK_ERROR", e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (HermesException e) {
            log.warn("[{}] {}: {}", LogEvents.SKILL_ACTIVATE, e.getErrorCode(), e.getMessage());
            return error(e.getErrorCode(), e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("[{}] UNKNOWN: {}", LogEvents.SKILL_ACTIVATE, e.getMessage(), e);
            return error("UNKNOWN", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/api/hermes/health")
    public ResponseEntity<Map<String, Object>> health() {
        log.info("[{}] checking...", LogEvents.HEALTH_CHECK);
        boolean healthy = hermesClient.healthCheck();
        Map<String, Object> body = new HashMap<>();
        body.put("status", healthy ? "UP" : "DOWN");
        body.put("timestamp", System.currentTimeMillis());
        
        log.info("[{}] result={}", LogEvents.HEALTH_CHECK, healthy ? "UP" : "DOWN");
        return healthy 
            ? ResponseEntity.ok(body) 
            : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
    
    private ResponseEntity<Map<String, Object>> success(String data) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("data", data);
        body.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(body);
    }
    
    private ResponseEntity<Map<String, Object>> error(String code, String message, HttpStatus status) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", code);
        body.put("message", message);
        body.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(status).body(body);
    }
    
    private String maskContent(String content) {
        if (content == null) return "null";
        return content.length() > 50 
            ? content.substring(0, 50) + "...(length=" + content.length() + ")" 
            : content;
    }
}