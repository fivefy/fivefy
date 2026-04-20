package com.fivefy.domain.pointorder.dto;

import com.fivefy.domain.pointorder.entity.PointOrder;
import com.fivefy.domain.pointorder.enums.PointOrderStatus;
import com.fivefy.domain.subscription.enums.SubscriptionPlanType;

public record PointOrderResponse(
        Long id,
        Long userId,
        SubscriptionPlanType planType,
        Long subscriptionAmount,
        String orderNumber,
        PointOrderStatus status
) {
    public static PointOrderResponse from(PointOrder pointOrder) {
        return new PointOrderResponse(
                pointOrder.getId(),
                pointOrder.getUserId(),
                pointOrder.getPlanType(),
                pointOrder.getSubscriptionAmount(),
                pointOrder.getOrderNumber(),
                pointOrder.getStatus()
        );
    }
}
