package com.hermes.sdk.webhook;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * HMAC-SHA256 签名工具
 *
 * 用于 Hermes Webhook 签名验证
 * 签名算法: HMAC-SHA256(secret, body)
 * 输出: 小写十六进制
 */
public class HMACSigner {

    private static final Logger log = LogManager.getLogger(HMACSigner.class);
    private static final String ALGORITHM = "HmacSHA256";

    /**
     * 对 body 进行 HMAC-SHA256 签名
     *
     * @param secret HMAC 密钥（不能为空）
     * @param body   要签名的内容（null 视为空字符串）
     * @return 小写十六进制签名（64 字符）
     * @throws IllegalArgumentException secret 为空时
     */
    public static String sign(String secret, String body) {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalArgumentException("HMAC secret must not be empty");
        }
        if (body == null) {
            body = "";
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            String signature = bytesToHex(hash);
            log.debug("HMAC signature generated: length={}", signature.length());
            return signature;
        } catch (Exception e) {
            throw new RuntimeException("HMAC signing failed: " + e.getMessage(), e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
