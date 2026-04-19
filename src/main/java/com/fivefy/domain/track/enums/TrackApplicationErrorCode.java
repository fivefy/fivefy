package com.fivefy.domain.track.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum TrackApplicationErrorCode implements ErrorCode {
    ERR_TRACK_APPLICATION_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "이미 처리된 트랙 등록 신청입니다"),
    ERR_TRACK_APPLICATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 처리 중인 트랙 등록 신청이 존재합니다");

    private final HttpStatus httpStatus;
    private final String message;

    TrackApplicationErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
