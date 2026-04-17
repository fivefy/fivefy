package com.fivefy.common.portone.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortoneWebhookRequest(
    String type,
    Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(String paymentId, String transactionId) {}
}