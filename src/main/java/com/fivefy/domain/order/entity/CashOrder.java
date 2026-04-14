package com.fivefy.domain.order.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.domain.order.enums.CashOrderStatus;
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

    @Column(nullable = false)
    private Long amount;

    @Column(length = 50, nullable = false, unique = true)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CashOrderStatus status;

    public static CashOrder create(Long userId, Long amount, String orderNumber) {
        validateNonNull(userId, "유저 ID");
        validatePositive(amount, "충전 금액");
        validateNonNull(orderNumber, "주문번호");

        CashOrder cashOrder = new CashOrder();
        cashOrder.userId = userId;
        cashOrder.amount = amount;
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