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
        private String orderNumber; // CashOrder에서 구매한 포인트 상품의 주문번호

        @Column(length = 100, nullable = false, unique = true)
        private String pgTransactionId; // << 이거 받아오기 : 포트원(PortOne)PG_ID

        @Column(length = 100, nullable = false, unique = true)
        private String webhookId;       // 멱등키 : CashOrderService에서 UUID.randomUUID().toString()으로 보냄

        @Column(length = 500)
        private String refundReason;

        private LocalDateTime paidAt;
        private LocalDateTime refundedAt;

        /**
         *
         * @param userId            : 유저 ID
         * @param amount            : 금액(원화 기준 : Long 통일)
         *        status            : 상태(결제 요청, 보류, 승인, 결제 완료, 실패, 취소, 환불)
         *        refundReason      : 환불사유 (글자 500 제한), 결제면 null
         * @param orderNumber       : 주문해서 구매한 포인트 패키지(실제돈->포인트) 주문 번호
         * @param pgTransactionId   : PG트랜젝션 ID - UUID 최대 글자 36자   : 포트원의 결제 ID (단건 조회, 취소에 사용)
         * @param webhookId         : 멱등키 - UUID 최대 글자 36자,         : 중복 처리 방지용 키
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
                payment.webhookId =  webhookId;   // 멱등키 idempotencyKey
                payment.paidAt = null;
                payment.refundedAt = null;

            return payment;
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