package com.hermes.sdk.transport;

import com.hermes.sdk.exception.HermesException;

/**
 * 传输层接口
 * 
 * 抽象 HTTP / RPC / WebSocket 等传输方式
 * 当前实现：HttpTransport
 * 未来扩展：RpcTransport / WebSocketTransport
 */
public interface Transport {
    
    /**
     * 发送 GET 请求
     */
    String get(String path) throws HermesException;
    
    /**
     * 发送 GET 请求（带查询参数）
     */
    String get(String path, java.util.Map<String, String> queryParams) throws HermesException;
    
    /**
     * 发送 POST 请求
     */
    String post(String path, Object body) throws HermesException;
    
    /**
     * 发送 PATCH 请求
     */
    String patch(String path, Object body) throws HermesException;
    
    /**
     * 发送 DELETE 请求
     */
    String delete(String path) throws HermesException;
    
    /**
     * 连接检查
     */
    boolean isConnected();
    
    /**
     * 关闭连接
     */
    void close();
}