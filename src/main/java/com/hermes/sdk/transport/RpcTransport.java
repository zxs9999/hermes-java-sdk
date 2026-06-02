package com.hermes.sdk.transport;

import com.hermes.sdk.exception.HermesException;

/**
 * RPC 传输层占位符
 * 
 * 未来实现 gRPC / Thrift / 自定义 RPC 协议
 * 
 * 实现步骤：
 * 1. 添加 grpc 依赖到 pom.xml
 * 2. 定义 .proto 文件
 * 3. 实现 stub 生成和 RpcTransport
 * 4. 在 HermesClient.Builder 添加 .transportType(TransportType.RPC)
 */
public class RpcTransport implements Transport {
    
    public RpcTransport(String host, int port) {
        throw new UnsupportedOperationException("RPC 传输层待实现");
    }
    
    @Override
    public String get(String path) throws HermesException {
        throw new UnsupportedOperationException("RPC 传输层待实现");
    }
    
    @Override
    public String get(String path, java.util.Map<String, String> queryParams) throws HermesException {
        throw new UnsupportedOperationException("RPC 传输层待实现");
    }
    
    @Override
    public String post(String path, Object body) throws HermesException {
        throw new UnsupportedOperationException("RPC 传输层待实现");
    }
    
    @Override
    public String patch(String path, Object body) throws HermesException {
        throw new UnsupportedOperationException("RPC 传输层待实现");
    }
    
    @Override
    public String delete(String path) throws HermesException {
        throw new UnsupportedOperationException("RPC 传输层待实现");
    }
    
    @Override
    public boolean isConnected() {
        return false;
    }
    
    @Override
    public void close() {
    }
}