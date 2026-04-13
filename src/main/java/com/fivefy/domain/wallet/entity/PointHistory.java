package com.fivefy.domain.wallet.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.domain.wallet.enums.PointType;
import com.fivefy.domain.wallet.enums.PointHistoryType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

@Entity
@Getter
@Table(name = "point_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long pointId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointType pointtype;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointHistoryType pointHistoryType;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Long balanceAfter;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static PointHistory create(Long pointId, PointType pointType, PointHistoryType pointHistoryType,
                                      Long amount, Long balanceAfter, String description) {
        validateNonNull(pointId, "pointId");
        validateNonNull(pointType, "pointType");
        validateNonNull(pointHistoryType, "pointHistoryType");
        validateNonNull(amount, "amount");
        validateNonNull(balanceAfter, "balanceAfter");
        validateNonNull(description, "description");

        PointHistory history = new PointHistory();
            history.pointId = pointId;
            history.pointtype = pointType;
            history.pointHistoryType = pointHistoryType;
            history.amount = amount;
            history.balanceAfter = balanceAfter;
            history.description = description;

        return history;
    }
}