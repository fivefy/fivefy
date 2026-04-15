package com.fivefy.domain.track.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum TrackErrorCode implements ErrorCode {
    ERR_TRACK_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지않는 트랙입니다"),
    ERR_TRACK_ALREADY_PUBLISHED(HttpStatus.BAD_REQUEST, "이미 공개된 트랙입니다"),
    ERR_TRACK_ALREADY_BLOCKED(HttpStatus.BAD_REQUEST, "이미 차단된 트랙입니다"),
    ERR_TRACK_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 트랙입니다"),
    ERR_TRACK_NOT_PUBLISHED(HttpStatus.BAD_REQUEST, "공개되지 않은 트랙입니다"),
    ERR_DELETED_TRACK_CANNOT_BE_UPDATED(HttpStatus.BAD_REQUEST, "삭제된 트랙은 수정할 수 없습니다"),
    ERR_INVALID_TRACK_NUMBER(HttpStatus.BAD_REQUEST, "트랙 번호는 1 이상이어야 합니다" ),
    ERR_INVALID_DURATION_SEC(HttpStatus.BAD_REQUEST, "총 재생 시간은 1 이상이어야 합니다");


    private final HttpStatus httpStatus;
    private final String message;

    TrackErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
