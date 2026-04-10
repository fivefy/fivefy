package com.fivefy.domain.order.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.domain.order.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

@Entity
@Getter
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long trackId;

    @Column(nullable = false)
    private Long totalAmount;

    @Column(nullable = false)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    public Order(Long userId, Long trackId, Long totalAmount, String orderNumber, OrderStatus status) {
        this.userId = userId;
        this.trackId = trackId;
        this.totalAmount = totalAmount;
        this.orderNumber = orderNumber;
        this.status = status;
    }

    public static Order create(Long userId, Long trackId, Long totalAmount, String orderNumber) {
        validateNonNull(userId, "유저 ID");
        validateNonNull(trackId, "트랙 ID");
        validateNonNull(totalAmount, "총 금액");
        validateNonNull(orderNumber, "주문 번호");

        Order order = new Order();
            order.userId = userId;
            order.trackId = trackId;
            order.totalAmount = totalAmount;
            order.orderNumber = orderNumber;
            order.status = OrderStatus.PENDING;

        return order;
    }
    public void updateStatus(OrderStatus status) {
        this.status = status;
    }
}
