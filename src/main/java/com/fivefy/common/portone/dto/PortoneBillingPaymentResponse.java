package com.fivefy.common.portone.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 포트원 빌링키 자동 청구 응답
 * POST /payments/{paymentId}/billing-key
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortoneBillingPaymentResponse(
        String paymentId,
        PaymentDetail payment  // 추가
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentDetail(
            String pgTxId,  // 2026-04-22 : 실제 PG 거래 ID
            String paidAt
    ) {}
}