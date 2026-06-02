package com.hermes.sdk.transport;

/**
 * 传输类型
 */
public enum TransportType {
    /** HTTP（默认）*/
    HTTP,
    /** RPC（gRPC/Thrift/自定义）*/
    RPC,
    /** WebSocket / SSE */
    WEBSOCKET
}