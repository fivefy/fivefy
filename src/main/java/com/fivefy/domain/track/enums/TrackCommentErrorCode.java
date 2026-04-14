package com.fivefy.domain.track.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum TrackCommentErrorCode implements ErrorCode {
    ERR_DELETED_TRACK_COMMENT_CANNOT_BE_UPDATED(HttpStatus.BAD_REQUEST, "삭제된 트랙 댓글은 수정할 수 없습니다"),
    ERR_TRACK_COMMENT_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 트랫 댓글입니다");


    private final HttpStatus httpStatus;
    private final String message;

    TrackCommentErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
