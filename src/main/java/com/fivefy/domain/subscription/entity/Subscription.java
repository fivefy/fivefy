package com.fivefy.domain.subscription.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.domain.subscription.enums.SubscriptionPlanType;
import com.fivefy.domain.subscription.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

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

    @Column(nullable = false)
    private Long pointOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlanType planType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status; // ACTIVE / INACTIVE / CANCELED / EXPIRE

    @Column(nullable = false)
    private LocalDateTime startDate; // 구매 시점

    private LocalDateTime expiryDate;     // 활성화 시점에 계산, 그 전까지 null
    private LocalDateTime nextBillingDate; // null이면 자동 갱신 없음 (취소된 상태)

    /**
     * 구독하기
     * 구독 즉시 ACTIVE, expiryDate/nextBillingDate 바로 세팅
     * @param userId             : 사용자 식별자
     * @param pointOrderId       : 포인트 주문 ID
     * @param planType           : 월, 년, 무료
     *        SubscriptionStatus : 체험, 활성, 만료, 취소
     * @param startDate          : 구독 시작일(테이블 생성일과 구조가 다름)
     *        expiryDate         : 구독 만료일
     *        nextBillingDate    : 구독 다음 결제일(이게 없으면 구독 취소한 것)
     * @return
     */
    public static Subscription create(
            Long userId,
            Long pointOrderId,
            SubscriptionPlanType planType,
            LocalDateTime startDate
    ) {
        validateNonNull(userId, "userId");
        validateNonNull(pointOrderId, "pointOrderId");
        validateNonNull(planType, "planType");
        validateNonNull(startDate, "startDate");

        Subscription subscription = new Subscription();
            subscription.userId = userId;
            subscription.pointOrderId = pointOrderId;
            subscription.planType = planType;
            subscription.status = SubscriptionStatus.ACTIVE;   // 바로 활성화

            subscription.startDate = startDate; // 시작

            // 만료일(구독일 + 1개월)
            subscription.expiryDate = planType.calculateExpiryDate(startDate);
            // 정기 구독 상태이면 다음 갱신일 1개월, 아니라면 null(구독 취소 된 상태. 남은 기간 이용 가능)
            subscription.nextBillingDate = (planType == SubscriptionPlanType.RECURRING)
                    ? startDate.plusMonths(1)
                    : null;
              // 테스트 용도
//            subscription.nextBillingDate = (planType == SubscriptionPlanType.RECURRING)
//                    ? subscription.expiryDate  // 만료일과 동일
//                    : null;

        return subscription;
    }

    /**
     * 구독 취소 - 다음 결제 중단, 만료일까지 이용 가능
     * FREE 취소 불가, ACTIVE 상태에서만 가능
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
     * 만료 처리 (스케줄러 또는 정기 결제 실패 시 호출)
     */
    public void expire() {
        this.status = SubscriptionStatus.EXPIRE;
        this.nextBillingDate = null;
    }

    /**
     * 정기 구독 갱신 (RECURRING 전용)
     * expiryDate      += 1개월
     * nextBillingDate  = 새 expiryDate 당일 00:00
     */
    public void renew() {
        if (this.planType != SubscriptionPlanType.RECURRING) {
            throw new IllegalStateException("RECURRING 플랜만 갱신할 수 있습니다.");
        }
        if (this.nextBillingDate == null) {
            throw new IllegalStateException("취소된 구독은 갱신할 수 없습니다.");
        }
        this.nextBillingDate = this.nextBillingDate.plusMonths(1);
        this.expiryDate      = this.expiryDate.plusMonths(1);
        this.status          = SubscriptionStatus.ACTIVE;
    }

    /**
     * 구독 활성화
     * @return
     */
    public boolean isActive() {
        return this.status == SubscriptionStatus.ACTIVE
                && this.expiryDate != null
                && LocalDateTime.now().isBefore(this.expiryDate);
    }

    // 플랜별 만료일은 SubscriptionPlanType 에서 관리


    /**
     * [테스트 전용] 1개월 갱신 시뮬레이션
     * 포인트 차감 없이 날짜만 앞당김
     * expiryDate      += 1개월
     * nextBillingDate  = 새 expiryDate 당일 00:00
     *
     * 예) 현재: startDate       = 4월 23일 19:07,
     *          expiryDate      = 5월 23일 19:07,
     *          nextBillingDate = 5월 23일 00:00
     *
     *     이후: startDate      = 4월 23일 19:07,
     *          expiryDate      = 6월 23일 19:07,
     *          nextBillingDate = 6월 23일 00:00
     */
    public void skipOneMonth() {
        if (this.expiryDate != null) {
            this.expiryDate = this.expiryDate.plusMonths(1);
            this.nextBillingDate = this.expiryDate.toLocalDate().atStartOfDay();
        }
    }
}