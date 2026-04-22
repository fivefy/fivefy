package com.fivefy.domain.cashorder.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CashProductType {

    PRODUCT_1(1000L, 1000L, "1,000원 → 1,000P"),
    PRODUCT_2(2000L, 2500L, "2,000원 → 2,500P"),
    PRODUCT_3(0L, 150L, "0원 → 150P"),
    PRODUCT_4(3000L, 4500L, "정기구독 최초 구매 (3개월치)"),      // 기간 3개월. 만료일 null, 다음 상품 구매하도록 설정
    PRODUCT_4_RECURRING(1000L, 1500L, "정기구독 월 자동 청구");  // 3개월이 지나면 활성화. 4개월차부터 자동 결제

    private final Long cashAmount;   // 실제 결제 금액 (원)
    private final Long pointAmount;  // 지급 포인트
    private final String description;
}