package com.fivefy.domain.album.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AlbumReleaseErrorCode implements ErrorCode {
    ERR_ALBUM_RELEASE_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "이미 처리된 앨범 등록 요청입니다"),
    ERR_ALBUM_RELEASE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 앨범 등록 요청입니다"),
    ERR_ALBUM_RELEASE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 처리중인 동일한 앨범 등록 요청입니다"),
    ERR_INVALID_PUBLISH_DELAY_DAYS(HttpStatus.BAD_REQUEST, "공개 예약일은 즉시 공개(0일) 또는 1일 후부터 7일 후까지만 선택할 수 있습니다"),
    ERR_INACTIVE_ARTIST_CANNOT_REQUEST_ALBUM_RELEASE(HttpStatus.BAD_REQUEST, "비활성화된 아티스트로는 앨범 등록 요청을 할 수 없습니다");

    private final HttpStatus httpStatus;
    private final String message;

    AlbumReleaseErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
