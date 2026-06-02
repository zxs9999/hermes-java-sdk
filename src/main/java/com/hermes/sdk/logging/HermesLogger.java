package com.hermes.sdk.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 统一日志获取入口
 * 
 * 所有类使用：HermesLogger.get(MyClass.class)
 * 替代 Lombok @Slf4j，保证日志名称一致
 */
public final class HermesLogger {
    
    private HermesLogger() {}
    
    /**
     * 获取 logger
     */
    public static Logger get(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
    
    /**
     * 获取 logger（传入类名字符串）
     */
    public static Logger get(String name) {
        return LoggerFactory.getLogger(name);
    }
}