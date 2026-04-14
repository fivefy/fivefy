package com.fivefy.domain.order.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.domain.order.enums.PointOrderStatus;
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
    private PointOrderStatus status;

    public static Order create(Long userId, Long trackId, Long totalAmount, String orderNumber) {
        validateNonNull(userId, "userId");
        validateNonNull(trackId, "trackId");
        validateNonNull(totalAmount, "totalAmount");
        validateNonNull(orderNumber, "orderNumber");

        Order order = new Order();
            order.userId = userId;
            order.trackId = trackId;
            order.totalAmount = totalAmount;
            order.orderNumber = orderNumber;
            order.status = PointOrderStatus.PENDING;

        return order;
    }
    public void updateStatus(PointOrderStatus status) {
        this.status = status;
    }
}
