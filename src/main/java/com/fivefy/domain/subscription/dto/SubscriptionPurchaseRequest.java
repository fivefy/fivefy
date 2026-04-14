package com.fivefy.domain.subscription.dto;

import com.fivefy.domain.subscription.enums.SubscriptionPlanType;
import jakarta.validation.constraints.NotNull;

/**
 * 구독 구매 요청
 * @param planType
 */
public record SubscriptionPurchaseRequest(
        @NotNull(message = "구독 플랜 타입은 필수입니다")
        SubscriptionPlanType planType   // MONTH, YEAR, FREE
) {}