package com.fivefy.domain.wallet.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.domain.wallet.enums.PointType;
import com.fivefy.domain.wallet.enums.PointHistoryType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

@Entity
@Getter
@Table(
    name = "point_histories",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"cash_order_id", "point_history_type"}),
        @UniqueConstraint(columnNames = {"point_order_id", "point_history_type"})
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointType pointType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointHistoryType pointHistoryType;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Long balanceAfter;

    @Column(nullable = false)
    private String logDescription;

    @Column
    private Long cashOrderId;   // CashOrder 관련 이력일 때 : 포인트 이력 추적용(충전)

    @Column
    private Long pointOrderId;  // PointOrder 관련 이력일 때 : 포인트 이력 추적용(사용)

    /**
     * 지갑 사용 내역
     * @param walletId          : 포인트 지갑 식별자
     * @param pointType         : 유료 재화, 무료 재화
     * @param pointHistoryType  : 사용, 환불, 충전
     * @param amount            : 변동 금액
     * @param balanceAfter      : 변동 후 내역
     * @param logDescription    : 로그(설명)
     * @return
     */
    public static PointHistory create(Long walletId, PointType pointType, PointHistoryType pointHistoryType,
                                      Long amount, Long balanceAfter, String logDescription,
                                      Long cashOrderId, Long pointOrderId
    ) {
        validateNonNull(walletId, "walletId");
        validateNonNull(pointType, "pointType");
        validateNonNull(pointHistoryType, "pointHistoryType");
        validateNonNull(amount, "amount");
        validateNonNull(balanceAfter, "balanceAfter");
        validateNonNull(logDescription, "logDescription");

        PointHistory history = new PointHistory();
            history.walletId = walletId;
            history.pointType = pointType;
            history.pointHistoryType = pointHistoryType;
            history.amount = amount;
            history.balanceAfter = balanceAfter;
            history.logDescription = logDescription;
            history.cashOrderId = cashOrderId;
            history.pointOrderId = pointOrderId;

        return history;
    }
}