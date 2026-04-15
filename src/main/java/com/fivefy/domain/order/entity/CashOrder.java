package com.fivefy.domain.order.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.domain.order.enums.CashOrderStatus;
import com.fivefy.domain.order.enums.CashProductType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;
import static com.fivefy.common.util.ValidationUtils.validatePositive;

@Entity
@Getter
@Table(name = "cash_orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CashOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /**
     * 구독 패키지 상품 번호(상점이 없으니 자체적으로 진행)
     * 1번 : 1,000원 = 1,000포인트
     * 2번 : 2,000원 = 2,500포인트
     * 3번 : 0원 = 150 포인트(계정당 1회 이용 가능)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CashProductType productType;

    @Column(nullable = false)
    private Long cashAmount;

    @Column(nullable = false)
    private Long pointAmount;

    @Column(length = 50, nullable = false, unique = true)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CashOrderStatus status;

    public static CashOrder create(Long userId, CashProductType productType, String orderNumber ) {
        validateNonNull(userId, "유저 ID");
        validateNonNull(productType, "상품 타입");
        validateNonNull(orderNumber, "주문번호");

        CashOrder cashOrder = new CashOrder();
            cashOrder.userId = userId;
            cashOrder.productType = productType;                    // 상품 타입
            cashOrder.cashAmount = productType.getCashAmount();     // 상품 타입 가격(원)
            cashOrder.pointAmount = productType.getPointAmount();   // 상품(포인트)
            cashOrder.orderNumber = orderNumber;
            cashOrder.status = CashOrderStatus.PENDING;

        return cashOrder;
    }

    public void success() {
        if (this.status != CashOrderStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 성공 처리할 수 있습니다.");
        }
        this.status = CashOrderStatus.SUCCESS;
    }

    public void refund() {
        if (this.status != CashOrderStatus.SUCCESS) {
            throw new IllegalStateException("SUCCESS 상태에서만 환불 처리할 수 있습니다.");
        }
        this.status = CashOrderStatus.REFUNDED;
    }
}