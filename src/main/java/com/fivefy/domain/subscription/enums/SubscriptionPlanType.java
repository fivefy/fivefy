package com.fivefy.domain.subscription.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubscriptionPlanType {

    MONTH(50L, "1달 구독"),
    YEAR(500L, "1년 구독"),
    FREE(0L, "3일 체험"),
    RECURRING(45L, "정기 구독 (매월)");

    private final Long price;
    private final String description;
}