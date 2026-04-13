package com.fivefy.common.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    ERR_EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "[A001] 만료된 JWT 토큰입니다"),
    ERR_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "[A002] 유효하지 않은 JWT 토큰입니다"),
    ERR_EMPTY_TOKEN(HttpStatus.UNAUTHORIZED, "[A003] JWT 토큰이 없습니다"),
    ERR_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "[A004] 인증되지 않은 사용자입니다");

    private final HttpStatus httpStatus;
    private final String message;
}
