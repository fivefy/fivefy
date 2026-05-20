package com.fivefy.domain.billingkey.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BillingKeyErrorCode implements ErrorCode {

    ERR_BILLING_KEY_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 카드입니다"),
    ERR_BILLING_KEY_NOT_FOUND(HttpStatus.NOT_FOUND, "빌링키를 찾을 수 없습니다"),
    ERR_BILLING_KEY_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 카드만 삭제할 수 있습니다"),
    ERR_BILLING_KEY_ALREADY_DEACTIVATED(HttpStatus.BAD_REQUEST, "이미 해지된 빌링키입니다"),
    ERR_BILLING_KEY_ACTIVE_NOT_FOUND(HttpStatus.NOT_FOUND, "활성 빌링키가 없습니다. 먼저 카드를 등록하세요"),
    ERR_BILLING_KEY_NEXT_CHARGE_DATE_NOT_FOUND(HttpStatus.BAD_REQUEST,"다음 자동충전 예정일이 설정되지 않았습니다");

    private final HttpStatus httpStatus;
    private final String message;
}