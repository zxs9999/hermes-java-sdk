package com.hermes.sdk.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hermes.sdk.config.HermesConfig;
import com.hermes.sdk.exception.HermesApiException;
import com.hermes.sdk.exception.HermesNetworkException;
import com.hermes.sdk.logging.HermesLogger;
import com.hermes.sdk.logging.LogEvents;
import okhttp3.*;

import java.io.IOException;

/**
 * Core API — Hermes 原始 API 封装
 * 
 * 一对一对应 gateway/platforms/api_server.py 的 HTTP 接口
 * 不做业务封装，只做 HTTP 调用 + 错误处理
 */
public class ApiServerCore {
    
    private static final Logger log = HermesLogger.get(ApiServerCore.class);
    
    protected final HermesConfig config;
    protected final OkHttpClient httpClient;
    protected final ObjectMapper mapper;
    
    public ApiServerCore(HermesConfig config, OkHttpClient httpClient, ObjectMapper mapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.mapper = mapper;
    }
    
    /**
     * GET 请求
     */
    protected String get(String path) throws HermesException {
        return get(path, (Map<String, String>) null);
    }
    
    protected String get(String path, Map<String, String> queryParams) throws HermesException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(config.getBaseUrl())
            .newBuilder()
            .addPathSegments(path.replaceFirst("^/", ""));
        
        if (queryParams != null) {
            queryParams.forEach(urlBuilder::addQueryParameter);
        }
        Request.Builder reqBuilder = new Request.Builder()
            .url(urlBuilder.build())
            .get();
        
        return execute(reqBuilder.build(), "GET", path);
    }
    
    /**
     * POST 请求（JSON body）
     */
    protected String post(String path, Object body) throws HermesException {
        String json;
        try {
            json = mapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new HermesApiException("JSON_ENCODE", -1, e.getMessage());
        }
        
        RequestBody requestBody = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder()
            .url(HttpUrl.parse(config.getBaseUrl()).newBuilder()
                .addPathSegments(path.replaceFirst("^/", "")).build())
            .post(requestBody)
            .build();
        
        return execute(request, "POST", path);
    }
    
    /**
     * PATCH 请求（JSON body）
     */
    protected String patch(String path, Object body) throws HermesException {
        String json;
        try {
            json = mapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new HermesApiException("JSON_ENCODE", -1, e.getMessage());
        }
        
        RequestBody requestBody = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder()
            .url(HttpUrl.parse(config.getBaseUrl()).newBuilder()
                .addPathSegments(path.replaceFirst("^/", "")).build())
            .patch(requestBody)
            .build();
        
        return execute(request, "PATCH", path);
    }
    
    /**
     * DELETE 请求
     */
    protected String delete(String path) throws HermesException {
        Request request = new Request.Builder()
            .url(HttpUrl.parse(config.getBaseUrl()).newBuilder()
                .addPathSegments(path.replaceFirst("^/", "")).build())
            .delete()
            .build();
        
        return execute(request, "DELETE", path);
    }
    
    /**
     * 执行请求
     */
    private String execute(Request request, String method, String path) throws HermesException {
        log.debug("[{}] {} {}", LogEvents.HTTP_REQUEST, method, path);
        
        try (Response resp = httpClient.newCall(request).execute()) {
            String body = resp.body().string();
            
            if (!resp.isSuccessful()) {
                log.error("[{}] API 错误: http={}, body={}", LogEvents.HTTP_ERROR, resp.code(), body);
                throw new HermesApiException(method + " " + path, resp.code(), body);
            }
            
            log.debug("[{}] 成功: http={}, bodyLen={}", LogEvents.HTTP_RESPONSE, resp.code(), body.length());
            return body;
            
        } catch (HermesApiException e) {
            throw e;
        } catch (IOException e) {
            log.error("[{}] 网络异常: {}", LogEvents.HTTP_ERROR, e.getMessage());
            throw new HermesNetworkException(method + " " + path, e);
        }
    }
}