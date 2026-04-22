package com.fivefy.domain.artist.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ArtistErrorCode implements ErrorCode {
    ERR_ARTIST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 아티스트입니다"),
    ERR_ARTIST_ALREADY_ACTIVATED(HttpStatus.BAD_REQUEST, "이미 활성화된 아티스트입니다"),
    ERR_ARTIST_ALREADY_INACTIVE(HttpStatus.BAD_REQUEST, "이미 비활성화된 아티스트입니다"),
    ERR_INVALID_ARTIST_NAME(HttpStatus.BAD_REQUEST, "아티스트 이름은 공백일 수 없습니다"),
    ERR_FORBIDDEN_ARTIST_ACCESS(HttpStatus.FORBIDDEN, "아티스트 소유자만 수정 또는 삭제할 수 있습니다");

    private final HttpStatus httpStatus;
    private final String message;

    ArtistErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
