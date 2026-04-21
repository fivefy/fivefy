package com.fivefy.domain.track.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum TrackCommentErrorCode implements ErrorCode {
    ERR_TRACK_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다"),
    ERR_TRACK_COMMENT_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 댓글입니다"),
    ERR_DELETED_TRACK_COMMENT_CANNOT_BE_UPDATED(HttpStatus.BAD_REQUEST, "삭제된 댓글은 수정할 수 없습니다"),
    ERR_FORBIDDEN_TRACK_COMMENT_UPDATE(HttpStatus.FORBIDDEN, "작성자만 댓글을 수정할 수 있습니다"),
    ERR_FORBIDDEN_TRACK_COMMENT_DELETE(HttpStatus.FORBIDDEN, "작성자 또는 관리자만 댓글을 삭제할 수 있습니다"),
    ERR_INVALID_TRACK_COMMENT_LENGTH(HttpStatus.BAD_REQUEST, "댓글은 1000자 이하여야 합니다");

    private final HttpStatus httpStatus;
    private final String message;

    TrackCommentErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}