package com.fivefy.domain.subscription.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 구독 환불 요청
 * @param subscriptionId
 */
public record SubscriptionRefundRequest(
        @NotNull(message = "구독 식별자는 필수입니다.")
        Long subscriptionId
) {}