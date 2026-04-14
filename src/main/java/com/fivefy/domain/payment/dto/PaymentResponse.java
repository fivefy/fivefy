package com.fivefy.domain.payment.dto;

import com.fivefy.domain.payment.entity.Payment;
import com.fivefy.domain.payment.enums.PaymentStatus;

import java.time.LocalDateTime;

/**
 * 결제 기록 응답
 * @param id
 * @param amount        : 금액
 * @param status        : 결제 상태(결제 요청, 보류, 승인, 결제, 실패, 취소, 환불)
 * @param paidAt        : 구매 일시
 * @param refundedAt    : 환불 일시
 */
public record PaymentResponse(
        Long id,
        Long amount,
        PaymentStatus status,
        LocalDateTime paidAt,
        LocalDateTime refundedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaidAt(),
                payment.getRefundedAt()
        );
    }
}