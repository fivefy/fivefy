package com.fivefy.domain.playback.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaybackErrorCode implements ErrorCode {

    PLAYBACK_NOT_FOUND(HttpStatus.NOT_FOUND, "재생 기록을 찾을 수 없습니다"),
    PLAYBACK_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 재생 기록에 접근할 수 없습니다"),
    CURRENT_PLAYBACK_NOT_FOUND(HttpStatus.CONFLICT, "현재 재생 중인 음악이 없습니다"),
    INVALID_PLAYBACK_STATE(HttpStatus.CONFLICT, "현재 재생 상태에서는 요청을 수행할 수 없습니다"),
    PLAYLIST_TRACK_NOT_FOUND(HttpStatus.NOT_FOUND, "플레이리스트 트랙 정보를 찾을 수 없습니다"),
    PLAYBACK_TRACK_MISMATCH(HttpStatus.CONFLICT, "현재 재생 정보와 플레이리스트 트랙 순서가 일치하지 않습니다");

    private final HttpStatus httpStatus;
    private final String message;
}
