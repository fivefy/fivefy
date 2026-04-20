package com.fivefy.domain.pointorder.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 구독 환불 요청
 * @param subscriptionId
 */
public record PointOrderRefundRequest(
        @NotNull(message = "구독 식별자는 필수입니다")
        Long subscriptionId
) {}
