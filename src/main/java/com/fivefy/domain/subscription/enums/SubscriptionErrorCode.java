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
    ERR_SUBSCRIPTION_ALREADY_CANCELED(HttpStatus.BAD_REQUEST, "취소된 구독은 갱신할 수 없습니다"),
    ERR_SUBSCRIPTION_ALREADY_EXPIRED(HttpStatus.BAD_REQUEST, "이미 만료된 구독입니다"),
    ERR_SUBSCRIPTION_UNSUPPORTED_STATUS(HttpStatus.BAD_REQUEST, "해당 구독 상태에서는 지원하지 않는 작업입니다"),
    ERR_SUBSCRIPTION_RECURRING_NOT_FOUND(HttpStatus.NOT_FOUND, "RECURRING(정기구독) + ACTIVE(활성화) 구독이 없습니다. 먼저 정기구독을 구매하세요");

    private final HttpStatus httpStatus;
    private final String message;
}