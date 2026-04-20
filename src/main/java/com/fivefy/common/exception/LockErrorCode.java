package com.fivefy.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum LockErrorCode implements ErrorCode {

    LOCK_ACQUISITION_FAILED(HttpStatus.CONFLICT, "현재 처리 중입니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus httpStatus;
    private final String message;
}