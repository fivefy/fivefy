package com.fivefy.domain.track.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum TrackApplicationErrorcode implements ErrorCode {
    ERR_TRACK_APPLICATION_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "이미 처리된 트랙 등록 요청입니다");

    private final HttpStatus httpStatus;
    private final String message;

    TrackApplicationErrorcode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
