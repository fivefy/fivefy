package com.fivefy.domain.follow.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FollowErrorCode implements ErrorCode {

    ERR_FOLLOW_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 팔로우한 아티스트입니다"),
    ERR_FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "팔로우가 존재하지 않습니다");

    private final HttpStatus httpStatus;
    private final String message;
}
