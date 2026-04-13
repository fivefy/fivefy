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

    private LocalDateTime nextBillingDate;

    /**
     * 구독
     * @param userId             : 유저 식별자
     * @param planType           : 월, 년, 무료
     *        SubscriptionStatus : 체험, 활성, 만료, 취소
     * @param startDate          : 구독 시작일(테이블 생성일과 구조가 다름)
     * @param expiryDate         : 구독 만료일
     * @param nextBillingDate    : 구독 다음 결제일(이게 없으면 구독 취소한 것)
     * @return
     */
    public static Subscription create(Long userId, SubscriptionPlanType planType, LocalDateTime startDate,
                                      LocalDateTime expiryDate, LocalDateTime nextBillingDate) {
        validateNonNull(userId, "userId");
        validateNonNull(planType, "planType");
        validateNonNull(startDate, "startDate");
        validateNonNull(expiryDate, "expiryDate");

        Subscription subscription = new Subscription();
            subscription.userId = userId;
            subscription.planType = planType;
            subscription.status = SubscriptionStatus.TRIAL;
            subscription.startDate = startDate;
            subscription.expiryDate = expiryDate;
            subscription.nextBillingDate = nextBillingDate;

        return subscription;
    }

    public void updateStatus(SubscriptionStatus status) {
        this.status = status;
    }
}