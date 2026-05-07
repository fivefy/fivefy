package com.fivefy.domain.billingattempt.enums;

public enum BillingFailureReason {
    CARD_DECLINED,          // 카드 거절 (한도 초과, 분실 신고 등)
    PG_TIMEOUT,             // PG사 타임아웃
    BILLING_KEY_INVALID,    // 빌링키 만료/삭제됨
    DB_ERROR,               // 포트원 성공 + DB 저장 실패 (제일 위험)
    UNKNOWN                 // 그 외
}