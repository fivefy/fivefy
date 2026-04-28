package com.fivefy.domain.payment.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    ERR_PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 결제 내역입니다"),
    ERR_PAYMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "본인 결제만 조회 가능합니다");

    private final HttpStatus httpStatus;
    private final String message;
}
