package com.fivefy.domain.payment.enums;

public enum PaymentStatus {
    REQUESTED,   // 결제 요청
    HOLD,        // 보류
    APPROVED,    // 승인      : PG사가 결제를 승인했지만 아직 최종 확정 전 단계(잘 안씀)
    COMPLETED,   // 결제 완료  : 서버에서 금액 검증, 웹훅 처리까지 모두 마친 최종 완료 상태
    FAILED,      // 실패
    CANCELED,    // 취소
    REFUNDED     // 환불
}
