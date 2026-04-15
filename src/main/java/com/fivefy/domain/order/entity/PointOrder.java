package com.fivefy.domain.order.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.domain.order.enums.PointOrderStatus;
import com.fivefy.domain.subscription.enums.SubscriptionPlanType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

@Entity
@Getter
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /**
     * 구독 플랜 타입
     * MONTH        : 1달
     * YEAR         : 1년
     * FREE         : 무료(1회 한정)
     * RECURRING    : 정기 구독(3개월 이후부터 구독 취소 가능)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlanType planType;

    @Column(nullable = false)
    private Long subscriptionAmount;

    /**
     * 구독 패키지 상품 번호(상점이 없으니 자체적으로 진행)
     * 1번 : 50 포인트 = 1달 이용
     * 2번 : 500포인트 = 1년 이용
     * 3번 : 0포인트 = 3일 이용(계정당 1회 한정)
     */
    @Column(nullable = false)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointOrderStatus status;

    public static PointOrder create(Long userId, SubscriptionPlanType planType, String orderNumber) {
        validateNonNull(userId, "userId");
        validateNonNull(planType, "planType");
        validateNonNull(orderNumber, "orderNumber");

        PointOrder pointOrder = new PointOrder();
            pointOrder.userId = userId;
            pointOrder.planType = planType;
            pointOrder.subscriptionAmount = planType.getPrice();
            pointOrder.orderNumber = orderNumber;
            pointOrder.status = PointOrderStatus.PENDING;

        return pointOrder;
    }

    public void success() {
        if (this.status != PointOrderStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 성공 처리할 수 있습니다.");
        }
        this.status = PointOrderStatus.SUCCESS;
    }

    public void refund() {
        if (this.status != PointOrderStatus.SUCCESS) {
            throw new IllegalStateException("SUCCESS 상태에서만 환불 처리할 수 있습니다.");
        }
        this.status = PointOrderStatus.REFUNDED;
    }
}
