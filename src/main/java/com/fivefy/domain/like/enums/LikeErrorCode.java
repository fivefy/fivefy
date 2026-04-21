package com.fivefy.domain.like.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum LikeErrorCode implements ErrorCode {

    ERR_LIKE_INVALID_TARGET_ID(HttpStatus.BAD_REQUEST, "유효하지 않은 타겟 ID입니다"),
    ERR_LIKE_INVALID_TARGET_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 타겟 타입입니다"),
    ERR_LIKE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 좋아요한 대상입니다"),
    ERR_LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "좋아요가 존재하지 않습니다");

    private final HttpStatus httpStatus;
    private final String message;
}
