package com.fivefy.domain.order.dto;

public record CashOrderRefundRequest(
        String orderNumber,
        String reason
) {}