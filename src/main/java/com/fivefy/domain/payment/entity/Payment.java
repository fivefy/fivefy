package com.fivefy.domain.payment.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.domain.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

@Entity
@Getter
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(length = 100, nullable = false)
    private String pgTransactionId;

    @Column(length = 100, nullable = false)
    private String idempotencyKey;

    @Column(length = 500)
    private String refundReason;

    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;

    /**
     * 결제
     * @param orderId           : 주문 ID
     * @param amount            : 금액(원화 기준 : Long 통일)
     * @param status            : 상태(결제 요청, 보류, 승인, 결제 완료, 실패, 취소, 환불)
     * @param pgTransactionId   : PG트랜젝션 ID - UUID 최대 글자 36자
     * @param refundReason      : 환불사유 (글자 500 제한), 결제면 null
     * @param idempotencyKey    : 멱등키 - UUID 최대 글자 36자
     * @param paidAt            : 결제 시각 : 환불이면 null
     * @param refundedAt        : 환불 시각 : 결제면 null
     */
    public Payment(Long orderId, Long amount, PaymentStatus status,
                   String pgTransactionId, String refundReason, String idempotencyKey,
                   LocalDateTime paidAt, LocalDateTime refundedAt) {
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.pgTransactionId = pgTransactionId;
        this.refundReason = refundReason;
        this.idempotencyKey = idempotencyKey;
        this.paidAt = paidAt;
        this.refundedAt = refundedAt;
    }

    public static Payment create(Long orderId, Long amount, String pgTransactionId, String idempotencyKey) {
        validateNonNull(orderId, "주문 ID");
        validateNonNull(amount, "결제 금액");
        validateNonNull(pgTransactionId, "PG 트랜잭션 ID");
        validateNonNull(idempotencyKey, "멱등키");

        Payment payment = new Payment();
            payment.orderId = orderId;
            payment.amount = amount;
            payment.status = PaymentStatus.REQUESTED;
            payment.pgTransactionId = pgTransactionId;
            payment.refundReason = null;
            payment.idempotencyKey =  idempotencyKey;
            payment.paidAt = LocalDateTime.now();
            payment.refundedAt = null;

        return payment;
    }

    public void updateStatus(PaymentStatus status) {
        this.status = status;
    }
}