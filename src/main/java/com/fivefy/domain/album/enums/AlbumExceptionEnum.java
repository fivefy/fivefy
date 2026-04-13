package com.fivefy.domain.album.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AlbumExceptionEnum implements ErrorCode {
    ERR_ALBUM_ALREADY_PUBLISHED(HttpStatus.BAD_REQUEST, "이미 발매된 앨범입니다"),
    ERR_ALBUM_ALREADY_BLOCKED(HttpStatus.BAD_REQUEST, "이미 차단된 앨범입니다"),
    ERR_ALBUM_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 앨범입니다"),
    ERR_DELETED_ALBUM_CANNOT_BE_UPDATED(HttpStatus.BAD_REQUEST, "삭제된 앨범은 수정할 수 없습니다"),
    ERR_INVALID_TRACK_COUNT(HttpStatus.BAD_REQUEST, "트랙 수는 0 이상이어야 합니다"),
    ERR_INVALID_TOTAL_DURATION_SEC(HttpStatus.BAD_REQUEST, "총 재생 시간은 0 이상이어야 합니다");

    private final HttpStatus httpStatus;
    private final String message;

    AlbumExceptionEnum(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
