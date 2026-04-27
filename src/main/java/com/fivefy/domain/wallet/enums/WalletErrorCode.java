package com.fivefy.domain.wallet.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WalletErrorCode implements ErrorCode {

    ERR_WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "지갑을 찾을 수 없습니다"),
    ERR_WALLET_PAID_BALANCE_INSUFFICIENT(HttpStatus.BAD_REQUEST, "유료 포인트가 부족합니다"),
    ERR_WALLET_FREE_BALANCE_INSUFFICIENT(HttpStatus.BAD_REQUEST, "무료 포인트가 부족합니다"),
    ERR_WALLET_TOTAL_BALANCE_INSUFFICIENT(HttpStatus.BAD_REQUEST, "포인트가 부족합니다");

    private final HttpStatus httpStatus;
    private final String message;
}