package com.fivefy.domain.order.dto;

import com.fivefy.domain.order.enums.CashProductType;

public record CashOrderVerifyRequest(
        CashProductType productType
) {}