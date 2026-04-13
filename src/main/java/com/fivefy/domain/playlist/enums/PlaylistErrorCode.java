package com.fivefy.domain.playlist.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaylistErrorCode implements ErrorCode {

    PLAYLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "플레이리스트를 찾을 수 없습니다"),
    INVALID_TITLE(HttpStatus.BAD_REQUEST, "플레이리스트 제목은 필수입니다"),
    PLAYLIST_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "플레이리스트 수정 권한이 없습니다"),
    PLAYLIST_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "플레이리스트 삭제 권한이 없습니다");

    private final HttpStatus httpStatus;
    private final String message;
}
