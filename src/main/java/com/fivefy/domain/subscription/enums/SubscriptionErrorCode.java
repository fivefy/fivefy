package com.fivefy.domain.subscription.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SubscriptionErrorCode implements ErrorCode {

    ERR_SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "활성 구독이 없습니다"),
    ERR_FREE_SUBSCRIPTION_CANNOT_CANCEL(HttpStatus.BAD_REQUEST, "무료 구독은 취소할 수 없습니다"),
    ERR_SUBSCRIPTION_INVALID_STATUS_CANCEL(HttpStatus.BAD_REQUEST, "활성 또는 비활성 상태에서만 취소할 수 있습니다"),
    ERR_SUBSCRIPTION_NOT_RECURRING(HttpStatus.BAD_REQUEST, "RECURRING 플랜만 갱신할 수 있습니다"),
    ERR_SUBSCRIPTION_ALREADY_CANCELED(HttpStatus.BAD_REQUEST, "취소된 구독은 갱신할 수 없습니다");

    private final HttpStatus httpStatus;
    private final String message;
}