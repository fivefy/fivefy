package com.fivefy.domain.subscription.dto;

import com.fivefy.domain.subscription.entity.Subscription;
import com.fivefy.domain.subscription.enums.SubscriptionPlanType;
import com.fivefy.domain.subscription.enums.SubscriptionStatus;

import java.time.LocalDateTime;

public record SubscriptionResponse(
        Long subscriptionId,
        SubscriptionPlanType planType,
        SubscriptionStatus status,
        LocalDateTime startDate,
        LocalDateTime expiryDate,
        LocalDateTime nextBillingDate,
        boolean active
) {
    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getPlanType(),
                subscription.getStatus(),
                subscription.getStartDate(),
                subscription.getExpiryDate(),
                subscription.getNextBillingDate(),
                subscription.isActive()
        );
    }
}