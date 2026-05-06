package com.fivefy.domain.billingattempt.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.domain.billingattempt.enums.BillingAttemptStatus;
import com.fivefy.domain.billingattempt.enums.BillingFailureReason;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Entity
@Getter
@Table(name = "billing_attempts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BillingAttempt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long subscriptionId;

    @Column(nullable = false)
    private Long billingKeyId;

    /**
     * UNIQUE 제약과 함께 월 중복 청구 방지
     */
    @Column(nullable = false, length = 7)
    private String billingCycle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillingAttemptStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private BillingFailureReason failureReason;  // 실패 시에만

    @Column(nullable = false)
    private LocalDateTime attemptedAt;

    private static final DateTimeFormatter CYCLE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM");

    public static BillingAttempt success(Long subscriptionId, Long billingKeyId) {
        BillingAttempt attempt = new BillingAttempt();
        attempt.subscriptionId = subscriptionId;
        attempt.billingKeyId   = billingKeyId;
        attempt.billingCycle   = YearMonth.now().format(CYCLE_FORMATTER);
        attempt.status         = BillingAttemptStatus.SUCCESS;
        attempt.failureReason  = null;
        attempt.attemptedAt    = LocalDateTime.now();
        return attempt;
    }

    public static BillingAttempt failure(Long subscriptionId, Long billingKeyId,
                                          BillingFailureReason reason) {
        BillingAttempt attempt = new BillingAttempt();
        attempt.subscriptionId = subscriptionId;
        attempt.billingKeyId   = billingKeyId;
        attempt.billingCycle   = YearMonth.now().format(CYCLE_FORMATTER);
        attempt.status         = BillingAttemptStatus.FAILED;
        attempt.failureReason  = reason;
        attempt.attemptedAt    = LocalDateTime.now();
        return attempt;
    }
}