package com.fivefy.domain.order.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.domain.order.enums.OrderEnum;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private OrderEnum status;

    public Order(Long userId, Long trackId, Long totalAmount, String orderNumber, OrderEnum status) {
        this.userId = userId;
        this.trackId = trackId;
        this.totalAmount = totalAmount;
        this.orderNumber = orderNumber;
        this.status = status;
    }

    public static Order create(Long userId, Long trackId, Long totalAmount, String orderNumber) {
        return new Order(
                userId,
                trackId,
                totalAmount,
                orderNumber,
                OrderEnum.PENDING
        );
    }

    public void updateStatus(OrderEnum status) {
        this.status = status;
    }

}
