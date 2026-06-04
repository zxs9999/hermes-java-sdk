package com.hermes.sdk.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Webhook 触发响应
 *
 * Hermes Gateway 在接收 Webhook 后会返回 202 Accepted 响应
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("delivery_id")
    private String deliveryId;

    @JsonProperty("message")
    private String message;

    public WebhookResponse() {}

    public WebhookResponse(String status, String deliveryId, String message) {
        this.status = status;
        this.deliveryId = deliveryId;
        this.message = message;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDeliveryId() { return deliveryId; }
    public void setDeliveryId(String deliveryId) { this.deliveryId = deliveryId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    @Override
    public String toString() {
        return "WebhookResponse{status=" + status +
               ", deliveryId=" + deliveryId +
               ", message=" + message + "}";
    }
}
