package com.fivefy.domain.user.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    ERR_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다"),
    ERR_USER_DUPLICATED_EMAIL(HttpStatus.CONFLICT, "중복된 이메일이 존재합니다"),
    ERR_USER_LOGIN_FAIL(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다"),
    ERR_USER_INVALID_RT(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다"),
    ERR_USER_EXPIRED_RT(HttpStatus.UNAUTHORIZED, "만료된 Refresh Token입니다"),
    ERR_USER_NOT_LOGGED_IN(HttpStatus.UNAUTHORIZED, "로그인 상태가 아닙니다"),
    ERR_USER_MISMATCH_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다");

    private final HttpStatus httpStatus;
    private final String message;
}
