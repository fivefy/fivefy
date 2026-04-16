package com.fivefy.domain.wallet.dto;

import com.fivefy.domain.wallet.entity.PointHistory;
import com.fivefy.domain.wallet.enums.PointHistoryType;
import com.fivefy.domain.wallet.enums.PointType;

import java.time.LocalDateTime;

public record PointHistoryResponse(
        Long id,
        PointType pointType,
        PointHistoryType pointHistoryType,
        Long amount,
        Long balanceAfter,
        String logDescription,
        LocalDateTime createdAt
) {
    public static PointHistoryResponse from(PointHistory history) {
        return new PointHistoryResponse(
                history.getId(),
                history.getPointType(),
                history.getPointHistoryType(),
                history.getAmount(),
                history.getBalanceAfter(),
                history.getLogDescription(),
                history.getCreatedAt()
        );
    }
}