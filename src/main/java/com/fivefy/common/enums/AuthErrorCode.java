package com.fivefy.common.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    ERR_AUTH_EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 JWT 토큰입니다"),
    ERR_AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 JWT 토큰입니다"),
    ERR_AUTH_EMPTY_TOKEN(HttpStatus.UNAUTHORIZED, "JWT 토큰이 없습니다"),
    ERR_AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "잘못된 접근입니다");

    private final HttpStatus httpStatus;
    private final String message;
}
