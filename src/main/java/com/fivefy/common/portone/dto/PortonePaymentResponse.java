package com.fivefy.common.portone.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortonePaymentResponse(
        String id,           // 포트원 결제 ID
        String status,       // PAID, FAILED, CANCELLED
        Amount amount,       // 결제 금액
        String orderName     // 주문명
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Amount(Long total) {}

    // 편의 메서드
    public Long totalAmount() {
        return amount != null ? amount.total() : null;
    }

    public String orderNumber() {
        return id;  // id가 orderNumber
    }
}