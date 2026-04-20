package com.fivefy.common.lock.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class ErrorEnum {

    // 락 획득 실패
    public static final ErrorEnum LOCK_ACQUISITION_FAILED =
            new ErrorEnum(HttpStatus.CONFLICT, "현재 처리 중입니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus httpStatus;
    private final String errorMessage;
}
