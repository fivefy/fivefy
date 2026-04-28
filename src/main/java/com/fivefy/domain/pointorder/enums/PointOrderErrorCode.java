package com.fivefy.domain.pointorder.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PointOrderErrorCode implements ErrorCode {

    ERR_FREE_PLAN_ALREADY_USED(HttpStatus.CONFLICT, "무료 체험은 1회만 가능합니다"),
    ERR_SUBSCRIPTION_ALREADY_ACTIVE(HttpStatus.CONFLICT, "이미 구독 중입니다"),
    ERR_POINT_ORDER_INVALID_STATUS_SUCCESS(HttpStatus.BAD_REQUEST, "PENDING 상태에서만 성공 처리할 수 있습니다"),
    ERR_POINT_ORDER_INVALID_STATUS_REFUND(HttpStatus.BAD_REQUEST, "SUCCESS 상태에서만 환불 처리할 수 있습니다");

    private final HttpStatus httpStatus;
    private final String message;
}