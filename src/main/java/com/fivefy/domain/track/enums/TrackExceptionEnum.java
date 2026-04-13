package com.fivefy.domain.track.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum TrackExceptionEnum implements ErrorCode {
    ERR_TRACK_ALREADY_PUBLISHED(HttpStatus.BAD_REQUEST, "이미 공개된 트랙입니다"),
    ERR_TRACK_ALREADY_BLOCKED(HttpStatus.BAD_REQUEST, "이미 차단된 트랙입니다"),
    ERR_TRACK_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 트랙입니다"),
    ERR_TRACK_NOT_PUBLISHED(HttpStatus.BAD_REQUEST, "공개되지 않은 트랙입니다"),
    ERR_DELETED_TRACK_CANNOT_BE_UPDATED(HttpStatus.BAD_REQUEST, "삭제된 앨범은 수정할 수 없습니다");

    private final HttpStatus httpStatus;
    private final String message;

    TrackExceptionEnum(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
