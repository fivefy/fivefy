package com.fivefy.domain.user.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    ERR_USER_DUPLICATED_EMAIL(HttpStatus.CONFLICT, "중복된 이메일이 존재합니다");

    private final HttpStatus httpStatus;
    private final String message;
}
