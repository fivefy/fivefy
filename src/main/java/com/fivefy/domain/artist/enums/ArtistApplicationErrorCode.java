package com.fivefy.domain.artist.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ArtistApplicationErrorCode implements ErrorCode {
    ERR_ARTIST_APPLICATION_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "이미 처리된 아티스트 등록 요청입니다"),
    ERR_ARTIST_APPLICATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 아티스트 등록 요청입니다"),
    ERR_ARTIST_APPLICATION_DETAIL_FORBIDDEN(HttpStatus.FORBIDDEN, "본인 또는 관리자만 아티스트 등록 요청을 상세 조회할 수 있습니다");

    private final HttpStatus httpStatus;
    private final String message;

    ArtistApplicationErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
