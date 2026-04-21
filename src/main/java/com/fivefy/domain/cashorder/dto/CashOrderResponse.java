package com.fivefy.domain.cashorder.dto;

import com.fivefy.domain.cashorder.entity.CashOrder;
import com.fivefy.domain.cashorder.enums.CashOrderStatus;
import com.fivefy.domain.cashorder.enums.CashProductType;

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