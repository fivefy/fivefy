package com.fivefy.domain.cashorder.dto;

import com.fivefy.domain.cashorder.enums.CashProductType;

public record CashOrderVerifyRequest(
        CashProductType productType
) {}