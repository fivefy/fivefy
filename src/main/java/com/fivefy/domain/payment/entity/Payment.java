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
    private Long userId;

    // 음수값 처리는 나중에
    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(length = 100, nullable = false, unique = true)
    private String pgTransactionId; // << 이거 받아오기 : 포트원(PortOne)PG_ID

    @Column(length = 100, nullable = false, unique = true)
    private String idempotencyKey;

    @Column(length = 500)
    private String refundReason;

    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;

    /**
     *
     * @param userId            : 유저 ID
     * @param amount            : 금액(원화 기준 : Long 통일)
     *        status            : 상태(결제 요청, 보류, 승인, 결제 완료, 실패, 취소, 환불)
     * @param pgTransactionId   : PG트랜젝션 ID - UUID 최대 글자 36자
     *        refundReason      : 환불사유 (글자 500 제한), 결제면 null
     * @param idempotencyKey    : 멱등키 - UUID 최대 글자 36자,
     *        paidAt            : 결제 시각 : 환불이면 null
     *        refundedAt        : 환불 시각 : 결제면 null
     * @return
     */
    public static Payment create(Long userId, Long amount, String pgTransactionId, String idempotencyKey) {
        validateNonNull(userId, "orderId");
        validateNonNull(amount, "amount");
        validateNonNull(pgTransactionId, "pgTransactionId");
        validateNonNull(idempotencyKey, "idempotencyKey");

        Payment payment = new Payment();
            payment.userId = userId;
            payment.amount = amount;
            payment.status = PaymentStatus.REQUESTED;
            payment.pgTransactionId = pgTransactionId;
            payment.refundReason = null;
            payment.idempotencyKey =  idempotencyKey;
            payment.paidAt = null;
            payment.refundedAt = null;

        return payment;
    }

    public void updateStatus(PaymentStatus status) {
        this.status = status;
    }

    public void complete() {
        this.status = PaymentStatus.COMPLETED;
        this.paidAt = LocalDateTime.now();
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
    }

    public void refund(String reason) {
        validateNonNull(reason, "환불 사유");
        this.status = PaymentStatus.REFUNDED;
        this.refundReason = reason;
        this.refundedAt = LocalDateTime.now();
    }
}