package com.fivefy.domain.chat.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatSessionErrorCode implements ErrorCode {

    ERR_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 세션입니다");

    private final HttpStatus httpStatus;
    private final String message;
}
