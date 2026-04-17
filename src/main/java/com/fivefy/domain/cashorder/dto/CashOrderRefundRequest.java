package com.fivefy.domain.cashorder.dto;

public record CashOrderRefundRequest(
        String orderNumber,
        String reason
) {}