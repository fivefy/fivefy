package com.fivefy.domain.artist.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ArtistErrorCode implements ErrorCode {

    ERR_ARTIST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 아티스트입니다");

    private final HttpStatus httpStatus;
    private final String message;
}
