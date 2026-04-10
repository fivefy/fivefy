package com.fivefy.domain.payment.enums;

public enum PaymentStatus {
    REQUESTED,   // 결제 요청
    HOLD,        // 보류
    APPROVED,    // 승인
    COMPLETED,   // 결제 완료
    FAILED,      // 실패
    CANCELED,    // 취소
    REFUNDED     // 환불
}
