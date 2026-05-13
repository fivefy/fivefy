package com.fivefy.domain.cashorder.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CashOrderErrorCode implements ErrorCode {

    ERR_CASH_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 주문입니다"),
    ERR_CASH_ORDER_FORBIDDEN(HttpStatus.FORBIDDEN, "본인 주문만 환불 가능합니다"),
    ERR_CASH_ORDER_INVALID_STATUS_SUCCESS(HttpStatus.BAD_REQUEST, "PENDING 상태에서만 성공 처리할 수 있습니다"),
    ERR_CASH_ORDER_INVALID_STATUS_REFUND(HttpStatus.BAD_REQUEST, "SUCCESS 상태에서만 환불 처리할 수 있습니다"),
    ERR_CASH_ORDER_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다"),
    ERR_CASH_ORDER_CANCEL_FAILED(HttpStatus.BAD_REQUEST, "포트원 결제 취소에 실패했습니다"),
    ERR_FREE_CASH_PRODUCT_ALREADY_USED(HttpStatus.CONFLICT, "150P 맛보기 포인트는 계정당 1회만 받을 수 있습니다"),
    ERR_CASH_ORDER_WEBHOOK_PARSE_FAILED(HttpStatus.BAD_REQUEST, "웹훅 body 파싱에 실패했습니다");

    private final HttpStatus httpStatus;
    private final String message;
}