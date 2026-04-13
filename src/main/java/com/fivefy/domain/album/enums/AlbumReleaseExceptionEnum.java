package com.fivefy.domain.album.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AlbumReleaseExceptionEnum implements ErrorCode {
    ERR_ALBUM_RELEASE_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "이미 처리된 앨범 등록 요청입니다");


    private final HttpStatus httpStatus;
    private final String message;

    AlbumReleaseExceptionEnum(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
