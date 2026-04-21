package com.fivefy.common.portone.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortoneCancelResponse(
        Cancellation cancellation
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cancellation(String status) {}

    // 편의 메서드
    public String status() {
        return cancellation != null ? cancellation.status() : null;
    }
}