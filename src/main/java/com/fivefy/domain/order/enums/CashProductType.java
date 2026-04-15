package com.fivefy.domain.order.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CashProductType {

    PRODUCT_1(1000L, 1000L, "1,000원 → 1,000P"),
    PRODUCT_2(2000L, 2500L, "2,000원 → 2,500P"),
    PRODUCT_3(0L, 150L, "0원 → 150P");


    private final Long cashAmount;   // 실제 결제 금액 (원)
    private final Long pointAmount;  // 지급 포인트
    private final String description;
}