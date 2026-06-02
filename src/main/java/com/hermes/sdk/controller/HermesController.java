package com.hermes.sdk.controller;

import com.hermes.sdk.client.HermesClient;
import com.hermes.sdk.dto.ChatRequest;
import com.hermes.sdk.dto.SkillRequest;
import com.hermes.sdk.exception.HermesApiException;
import com.hermes.sdk.exception.HermesAuthException;
import com.hermes.sdk.exception.HermesException;
import com.hermes.sdk.exception.HermesNetworkException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RestController
@RequestMapping("/api/hermes")
@RequiredArgsConstructor
@Validated
public class HermesController {
    
    private final HermesClient hermesClient;
    
    /**
     * 聊天
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@Valid @RequestBody ChatRequest request) {
        try {
            String result = hermesClient.chat(request.getMessage());
            return success(result);
        } catch (HermesAuthException e) {
            return error("AUTH_ERROR", e.getMessage(), HttpStatus.UNAUTHORIZED);
        } catch (HermesNetworkException e) {
            return error("NETWORK_ERROR", e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (HermesException e) {
            return error(e.getErrorCode(), e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("未知异常", e);
            return error("UNKNOWN", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 激活 Skill
     */
    @PostMapping("/skill")
    public ResponseEntity<Map<String, Object>> skill(@Valid @RequestBody SkillRequest request) {
        try {
            String result = hermesClient.activateSkill(request.getSkillName(), request.getTask());
            return success(result);
        } catch (HermesAuthException e) {
            return error("AUTH_ERROR", e.getMessage(), HttpStatus.UNAUTHORIZED);
        } catch (HermesNetworkException e) {
            return error("NETWORK_ERROR", e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (HermesException e) {
            return error(e.getErrorCode(), e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("未知异常", e);
            return error("UNKNOWN", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean healthy = hermesClient.healthCheck();
        Map<String, Object> body = new HashMap<>();
        body.put("status", healthy ? "UP" : "DOWN");
        body.put("timestamp", System.currentTimeMillis());
        
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
        log.warn("Hermes API 错误: {} - {}", code, message);
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", code);
        body.put("message", message);
        body.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(status).body(body);
    }
}