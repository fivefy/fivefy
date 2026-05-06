package com.fivefy.common.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AlgorithmErrorCode implements ErrorCode {

    ERR_ALGORITHM_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "지원하지 않는 암호화 알고리즘입니다");

    private final HttpStatus httpStatus;
    private final String message;
}
