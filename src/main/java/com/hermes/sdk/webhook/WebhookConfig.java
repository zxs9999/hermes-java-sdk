package com.hermes.sdk.webhook;

/**
 * Webhook 客户端配置
 *
 * 使用 Builder 模式配置 Webhook 连接参数
 */
public class WebhookConfig {

    private final String baseUrl;
    private final String secret;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final int writeTimeoutMs;
    private final int maxRetries;
    private final long retryBackoffMs;

    private WebhookConfig(Builder b) {
        this.baseUrl = b.baseUrl;
        this.secret = b.secret;
        this.connectTimeoutMs = b.connectTimeoutMs;
        this.readTimeoutMs = b.readTimeoutMs;
        this.writeTimeoutMs = b.writeTimeoutMs;
        this.maxRetries = b.maxRetries;
        this.retryBackoffMs = b.retryBackoffMs;
    }

    public String getBaseUrl() { return baseUrl; }
    public String getSecret() { return secret; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public int getWriteTimeoutMs() { return writeTimeoutMs; }
    public int getMaxRetries() { return maxRetries; }
    public long getRetryBackoffMs() { return retryBackoffMs; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String baseUrl;
        private String secret;
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 10000;
        private int writeTimeoutMs = 5000;
        private int maxRetries = 2;
        private long retryBackoffMs = 1000;

        public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }
        public Builder secret(String secret) { this.secret = secret; return this; }
        public Builder connectTimeoutMs(int ms) { this.connectTimeoutMs = ms; return this; }
        public Builder readTimeoutMs(int ms) { this.readTimeoutMs = ms; return this; }
        public Builder writeTimeoutMs(int ms) { this.writeTimeoutMs = ms; return this; }
        public Builder maxRetries(int retries) { this.maxRetries = retries; return this; }
        public Builder retryBackoffMs(long ms) { this.retryBackoffMs = ms; return this; }

        /**
         * 禁用重试退避（测试用，避免 sleep 拖慢测试）
         */
        public Builder noRetryBackoff() { this.retryBackoffMs = 0; return this; }

        public WebhookConfig build() {
            if (baseUrl == null || baseUrl.isEmpty()) {
                throw new IllegalArgumentException("baseUrl must not be empty");
            }
            if (secret == null || secret.isEmpty()) {
                throw new IllegalArgumentException("secret must not be empty");
            }
            if (maxRetries < 0) {
                throw new IllegalArgumentException("maxRetries must be >= 0");
            }
            if (retryBackoffMs < 0) {
                throw new IllegalArgumentException("retryBackoffMs must be >= 0");
            }
            // 去掉尾部斜杠
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            return new WebhookConfig(this);
        }
    }
}
