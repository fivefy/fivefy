package com.fivefy.domain.pointorder.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.pointorder.enums.PointOrderErrorCode;
import com.fivefy.domain.pointorder.enums.PointOrderStatus;
import com.fivefy.domain.subscription.enums.SubscriptionPlanType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

@Entity
@Getter
@Table(name = "point_orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /**
     * 구독 플랜 타입
     * FREE         : 0P    : 무료(1회 한정)
     * RECURRING    : 50P   : 정기 구독
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlanType planType;

    @Column(nullable = false)
    private Long subscriptionAmount;

    /**
     * 구독 패키지 상품 번호(상점이 없으니 자체적으로 진행)
     * 자체 주문번호 ("SUB-" + UUID 앞 8자리)
     */
    @Column(nullable = false)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointOrderStatus status;

    /**
     * 구독 주문 생성 매서드
     *
     * @param userId
     * @param planType
     * @param orderNumber
     * @return
     */
    public static PointOrder create(Long userId, SubscriptionPlanType planType, String orderNumber) {
        validateNonNull(userId, "userId");
        validateNonNull(planType, "planType");
        validateNonNull(orderNumber, "orderNumber");

        PointOrder pointOrder = new PointOrder();
            pointOrder.userId = userId;
            pointOrder.planType = planType;
            pointOrder.subscriptionAmount = planType.getPrice(); // 구매 시점 가격 확정
            pointOrder.orderNumber = orderNumber;
            pointOrder.status = PointOrderStatus.PENDING;

        return pointOrder;
    }

    /**
     * 주문 완료 처리 (PENDING → SUCCESS)
     * 외부 PG 없이 내부 처리이므로 PENDING → SUCCESS 즉시 전환
     */
    public void success() {
        if (this.status != PointOrderStatus.PENDING) {
            throw new BusinessException(PointOrderErrorCode.ERR_POINT_ORDER_INVALID_STATUS_SUCCESS);
        }
        this.status = PointOrderStatus.SUCCESS;
    }

    /**
     * 주문 환불 처리 (SUCCESS → REFUNDED)
     * PointOrderService.refund()에서 subscription.refund() 후 호출
     */
    public void refund() {
        if (this.status != PointOrderStatus.SUCCESS) {
            throw new BusinessException(PointOrderErrorCode.ERR_POINT_ORDER_INVALID_STATUS_REFUND);
        }
        this.status = PointOrderStatus.REFUNDED;
    }
}
