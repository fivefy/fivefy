package com.fivefy.domain.popularchart.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PopularChartErrorCode implements ErrorCode {

    CHART_NOT_FOUND(HttpStatus.NOT_FOUND, "차트 데이터가 존재하지 않습니다"),
    INVALID_SNAPSHOT_DATE(HttpStatus.BAD_REQUEST, "snapshotDate 형식이 올바르지 않습니다");

    private final HttpStatus httpStatus;
    private final String message;
}
