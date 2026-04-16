package com.fivefy.domain.subscription.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.subscription.enums.SubscriptionPlanType;
import com.fivefy.domain.subscription.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;
import static com.fivefy.domain.subscription.enums.UserErrorCode.ERR_TYPE_BAD_REQUEST;

@Entity
@Getter
@Table(name = "subscriptions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    // 테스트 : @Column(nullable = false)
    @Column // 테스트 : pointOrderId 임의로 집어넣음
    private Long pointOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlanType planType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    // 구독 취소 시 null
    private LocalDateTime nextBillingDate;

    /**
     * 구독
     * @param userId             : 사용자 식별자
     * @param pointOrderId       : 포인트 주문 ID
     * @param planType           : 월, 년, 무료
     *        SubscriptionStatus : 체험, 활성, 만료, 취소
     * @param startDate          : 구독 시작일(테이블 생성일과 구조가 다름)
     * @param expiryDate         : 구독 만료일
     * @param nextBillingDate    : 구독 다음 결제일(이게 없으면 구독 취소한 것)
     * @return
     */
    public static Subscription create(Long userId, Long pointOrderId, SubscriptionPlanType planType, LocalDateTime startDate,
                                      LocalDateTime expiryDate, LocalDateTime nextBillingDate) {
        validateNonNull(userId, "userId");
        // 테스트 : validateNonNull(pointOrderId, "pointOrderId");
        validateNonNull(planType, "planType");
        validateNonNull(startDate, "startDate");
        validateNonNull(expiryDate, "expiryDate");

        Subscription subscription = new Subscription();
            subscription.userId = userId;
            subscription.pointOrderId = pointOrderId;
            subscription.planType = planType;
            subscription.status = SubscriptionStatus.TRIAL;
            subscription.startDate = startDate;
            subscription.expiryDate = expiryDate;
            subscription.nextBillingDate = nextBillingDate;

        // 구독 시, FREE는 바로 활성화, 나머지는 비활성화
        if (planType == SubscriptionPlanType.FREE) {
            subscription.status = SubscriptionStatus.ACTIVE;
        } else {
            subscription.status = SubscriptionStatus.INACTIVE;
        }

        return subscription;
    }

    /**
     * 구독 환불 - 포인트 반환 대상(비활성 구독)
     */
    public void refund() {
        // 활성화 거르기
        if (this.status != SubscriptionStatus.INACTIVE) {
            throw new BusinessException(ERR_TYPE_BAD_REQUEST);
        }
        // 환불 시 타입 변경
        this.status = SubscriptionStatus.CANCELED;
        this.nextBillingDate = null;
    }

    /**
     * 구독 취소 - 다음 결제 중단, 만료일까지는 이용 가능
     */
    public void cancel() {
        if (this.planType == SubscriptionPlanType.FREE) {
            throw new IllegalStateException("무료 구독은 취소할 수 없습니다");
        }
        if (this.status != SubscriptionStatus.ACTIVE && this.status != SubscriptionStatus.INACTIVE) {
            throw new IllegalStateException("활성/비활성 상태에서만 취소할 수 있습니다 현재: " + this.status);
        }

        this.status = SubscriptionStatus.CANCELED;
        this.nextBillingDate = null;
    }

    /**
     * 만료 처리(쓸 예정은 없음)
     */
    public void expire() {
        this.status = SubscriptionStatus.EXPIRE;
        this.nextBillingDate = null;
    }

    /**
     * 활성화
     * @return
     */
    public boolean isActive() {
        return this.status == SubscriptionStatus.ACTIVE && LocalDateTime.now().isBefore(this.expiryDate);
    }

    /**
     * 플랜별 만료일 계산
     */
    public static LocalDateTime calculateExpiryDate(
            SubscriptionPlanType planType,
            LocalDateTime startDate
    ) {
        return switch (planType) {
            case MONTH, RECURRING -> startDate.plusMonths(1);
            case YEAR -> startDate.plusYears(1);
            case FREE -> startDate.plusDays(3);
        };
    }

    /**
     * 플랜별 다음 결제일 계산 (정기 구독만 해당)
     */
    public static LocalDateTime calculateNextBillingDate(
            SubscriptionPlanType planType,
            LocalDateTime startDate
    ) {
        return switch (planType) {
            case RECURRING -> startDate.plusMonths(1);
            case MONTH, YEAR, FREE -> null;
        };
    }
}