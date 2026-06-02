package com.hermes.sdk.transport;

import com.hermes.sdk.exception.HermesException;

/**
 * WebSocket 传输层占位符
 * 
 * 未来实现 WebSocket / SSE 实时通讯
 * 
 * 实现步骤：
 * 1. 添加 okHttp websocket 依赖
 * 2. 实现 WebSocketListener 回调
 * 3. 实现 WebSocketTransport
 * 4. 在 HermesClient.Builder 添加 .transportType(TransportType.WEBSOCKET)
 */
public class WebSocketTransport implements Transport {
    
    public WebSocketTransport(String url) {
        throw new UnsupportedOperationException("WebSocket 传输层待实现");
    }
    
    @Override
    public String get(String path) throws HermesException {
        throw new UnsupportedOperationException("WebSocket 传输层待实现");
    }
    
    @Override
    public String get(String path, java.util.Map<String, String> queryParams) throws HermesException {
        throw new UnsupportedOperationException("WebSocket 传输层待实现");
    }
    
    @Override
    public String post(String path, Object body) throws HermesException {
        throw new UnsupportedOperationException("WebSocket 传输层待实现");
    }
    
    @Override
    public String patch(String path, Object body) throws HermesException {
        throw new UnsupportedOperationException("WebSocket 传输层待实现");
    }
    
    @Override
    public String delete(String path) throws HermesException {
        throw new UnsupportedOperationException("WebSocket 传输层待实现");
    }
    
    @Override
    public boolean isConnected() {
        return false;
    }
    
    @Override
    public void close() {
    }
}