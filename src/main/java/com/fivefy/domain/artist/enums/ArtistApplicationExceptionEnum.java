package com.fivefy.domain.artist.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ArtistApplicationExceptionEnum implements ErrorCode {
    ERR_ARTIST_APPLICATION_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "이미 처리된 아티스트 등록 요청입니다");

    private final HttpStatus httpStatus;
    private final String message;

    ArtistApplicationExceptionEnum(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
