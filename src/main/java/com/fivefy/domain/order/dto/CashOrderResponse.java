package com.fivefy.domain.order.dto;

import com.fivefy.domain.order.entity.CashOrder;
import com.fivefy.domain.order.enums.CashOrderStatus;
import com.fivefy.domain.order.enums.CashProductType;

public record CashOrderResponse(
        Long cashOrderId,
        String orderNumber,
        CashProductType productType,
        Long cashAmount,
        Long pointAmount,
        CashOrderStatus status
) {
    public static CashOrderResponse from(CashOrder cashOrder) {
        return new CashOrderResponse(
                cashOrder.getId(),
                cashOrder.getOrderNumber(),
                cashOrder.getProductType(),
                cashOrder.getCashAmount(),
                cashOrder.getPointAmount(),
                cashOrder.getStatus()
        );
    }
}