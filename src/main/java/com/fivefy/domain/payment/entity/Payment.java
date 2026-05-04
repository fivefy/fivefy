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

    @Column(length = 50, nullable = false)
    private String orderNumber;     // CashOrder.orderNumber와 연결 (주문번호, 조회·환불 시 사용)

    @Column(length = 100, nullable = false, unique = true)
    private String pgTransactionId; // 포트원 결제 고유 ID — 환불 시 cancelPayment()에 사용

    @Column(length = 100, nullable = false, unique = true)
    private String webhookId;       // 멱등키 — CashOrder.webhookId와 동일값, 중복 처리 방지

    @Column(length = 500)
    private String refundReason;    // 환불 사유 (결제 시 null, 환불 시 기록)

    private LocalDateTime paidAt;       // 결제 완료 시각 (환불이면 null)
    private LocalDateTime refundedAt;   // 환불 완료 시각 (결제면 null)

    /**
     * 결제 기록
     * processWebhook()에서 금액 검증 통과 후 호출
     * @param userId            : 유저 ID
     * @param amount            : 금액(원화 기준 : Long 통일)
     *        status            : 상태(결제 요청(기본), 보류, 승인, 결제 완료, 실패, 취소, 환불)
     *        refundReason      : 환불사유 (글자 500 제한), 결제면 null
     * @param orderNumber       : 주문해서 구매한 포인트 패키지(실제돈->포인트) 주문 번호
     * @param pgTransactionId   : 포트원의 결제 ID (단건 조회, 취소에 사용)
     * @param webhookId         : 포트원 웹훅 고유 ID (멱등키, 중복 결제 방지)
     *        paidAt            : 결제 시각 : 환불이면 null
     *        refundedAt        : 환불 시각 : 결제면 null
     * @return
     */
    public static Payment create(Long userId, Long amount, String orderNumber, String pgTransactionId, String webhookId) {
        validateNonNull(userId, "orderId");
        validateNonNull(amount, "amount");
        validateNonNull(orderNumber, "orderNumber");
        validateNonNull(pgTransactionId, "pgTransactionId");
        validateNonNull(webhookId, "webhookId");

        Payment payment = new Payment();
            payment.userId = userId;
            payment.amount = amount;
            payment.orderNumber = orderNumber;
            payment.status = PaymentStatus.REQUESTED;
            payment.refundReason = null;
            payment.pgTransactionId = pgTransactionId;
            payment.webhookId = webhookId;   // 멱등키 idempotencyKey
            payment.paidAt = null;            // complete() 호출 시 기록
            payment.refundedAt = null;        // refund() 호출 시 기록

        return payment;
    }
    /**
     * 결제 완료 처리 (REQUESTED → COMPLETED)
     * create() 직후 processWebhook()에서 호출
     * 결제시각(paidAt) 기록
     *
     * 허용: REQUESTED → COMPLETED (일반 경로)
     *      COMPLETED → COMPLETED (재호출 허용 — 멱등 처리)
     * 차단: REFUNDED (환불 완료 후 재결제 불가)
     *
     * 조건 검사는 PaymentStatus.canTransitTo()가 담당
     */
    public void complete() {
        // 조건 검사
        this.status.transit(this, PaymentStatus.COMPLETED);

        if (this.paidAt == null) {
            this.paidAt = LocalDateTime.now();  // 최초 완료 시각만 기록
        }
    }

    /**
     * 결제 실패 시(아마 안쓸 거 같음. 필요 없으면 지우자) : (미적용)(2026-04-17)
     */
    public void fail() {
        this.status.transit(this, PaymentStatus.FAILED);
    }
    /**
     * 환불 처리 (COMPLETED → REFUNDED)
     * CashOrderService.refund()에서 포트원 취소 API 성공 확인 후 호출
     *
     * 허용: REQUESTED → REFUNDED (예외적 강제 환불)
     *       COMPLETED → REFUNDED (일반 환불 경로)
     * 차단: REFUNDED (이미 환불 완료)
     *
     * 환불 이유, 환불 시각 기록
     *
     *  조건 검사는 PaymentStatus.canTransitTo()가 담당
     */
    public void refund(String reason) {
        validateNonNull(reason, "환불 사유");
        this.refundReason = reason;
        this.status.transit(this, PaymentStatus.REFUNDED);
        this.refundedAt = LocalDateTime.now();
    }

    // 실패
    public void applyFail() {
        this.status = PaymentStatus.FAILED;
    }

    // 환불
    public void applyRefund() {
        this.status = PaymentStatus.REFUNDED;
        this.refundedAt = LocalDateTime.now();
    }

    // 성공
    public void applyComplete() {
        this.status = PaymentStatus.COMPLETED;
    }
}