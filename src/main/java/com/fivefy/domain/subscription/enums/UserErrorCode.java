package com.fivefy.domain.subscription.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    ERR_TYPE_BAD_REQUEST(HttpStatus.BAD_REQUEST, "비활성 상태인 구독만 환불할 수 있습니다");

    private final HttpStatus httpStatus;
    private final String message;
}
