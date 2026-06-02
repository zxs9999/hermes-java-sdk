package com.hermes.sdk.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hermes.sdk.config.HermesConfig;
import com.hermes.sdk.exception.HermesApiException;
import com.hermes.sdk.exception.HermesNetworkException;
import com.hermes.sdk.logging.HermesLogger;
import com.hermes.sdk.logging.LogEvents;
import okhttp3.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Map;

/**
 * HTTP 传输层实现
 * 
 * 实现 Transport 接口，提供 HTTP 通讯能力
 * 可替换为 RpcTransport / WebSocketTransport
 */
public class HttpTransport implements Transport {
    
    private static final Logger log = HermesLogger.get(HttpTransport.class);
    
    private final HermesConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;
    
    public HttpTransport(HermesConfig config, OkHttpClient httpClient, ObjectMapper mapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.mapper = mapper;
    }
    
    @Override
    public String get(String path) throws HermesException {
        return get(path, null);
    }
    
    @Override
    public String get(String path, Map<String, String> queryParams) throws HermesException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(config.getBaseUrl())
            .newBuilder()
            .addPathSegments(path.replaceFirst("^/", ""));
        
        if (queryParams != null) {
            queryParams.forEach(urlBuilder::addQueryParameter);
        }
        
        Request request = new Request.Builder()
            .url(urlBuilder.build())
            .get()
            .build();
        
        return execute(request, "GET", path);
    }
    
    @Override
    public String post(String path, Object body) throws HermesException {
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
    
    @Override
    public String patch(String path, Object body) throws HermesException {
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
    
    @Override
    public String delete(String path) throws HermesException {
        Request request = new Request.Builder()
            .url(HttpUrl.parse(config.getBaseUrl()).newBuilder()
                .addPathSegments(path.replaceFirst("^/", "")).build())
            .delete()
            .build();
        
        return execute(request, "DELETE", path);
    }
    
    @Override
    public boolean isConnected() {
        try {
            Request request = new Request.Builder()
                .url(config.getBaseUrl() + "/health")
                .get()
                .build();
            
            try (Response resp = httpClient.newCall(request).execute()) {
                return resp.isSuccessful();
            }
        } catch (IOException e) {
            return false;
        }
    }
    
    @Override
    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
    
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