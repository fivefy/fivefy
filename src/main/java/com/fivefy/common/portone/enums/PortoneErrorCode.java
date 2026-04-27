package com.fivefy.common.portone.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PortoneErrorCode implements ErrorCode {

    ERR_PORTONE_PAYMENT_NOT_FOUND(HttpStatus.BAD_REQUEST, "포트원 결제 조회에 실패했습니다"),
    ERR_PORTONE_CANCEL_FAILED(HttpStatus.BAD_REQUEST, "포트원 환불에 실패했습니다"),
    ERR_PORTONE_BILLING_KEY_NOT_FOUND(HttpStatus.BAD_REQUEST, "포트원 빌링키 조회에 실패했습니다"),
    ERR_PORTONE_BILLING_CHARGE_FAILED(HttpStatus.BAD_REQUEST, "빌링키 자동 청구에 실패했습니다"),
    ERR_PORTONE_BILLING_KEY_DELETE_FAILED(HttpStatus.BAD_REQUEST, "포트원 빌링키 삭제에 실패했습니다");

    private final HttpStatus httpStatus;
    private final String message;
}