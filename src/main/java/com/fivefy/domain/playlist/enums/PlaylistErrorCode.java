package com.fivefy.domain.playlist.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaylistErrorCode implements ErrorCode {

    // Playlist
    PLAYLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "플레이리스트를 찾을 수 없습니다"),
    INVALID_TITLE(HttpStatus.BAD_REQUEST, "플레이리스트 제목은 필수입니다"),
    DUPLICATE_PLAYLIST_NAME(HttpStatus.CONFLICT, "이미 동일한 이름의 플레이리스트가 존재합니다"),
    PLAYLIST_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "플레이리스트 수정 권한이 없습니다"),
    PLAYLIST_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "플레이리스트 삭제 권한이 없습니다"),
    ALREADY_DELETED_PLAYLIST(HttpStatus.BAD_REQUEST, "이미 삭제된 플레이리스트입니다"),
    PLAYLIST_CREATION_SUBSCRIPTION_REQUIRED(HttpStatus.FORBIDDEN, "플레이리스트 생성은 구독 회원만 가능합니다"),

    // PlaylistTrack
    PLAYLIST_ACCESS_FORBIDDEN(HttpStatus.FORBIDDEN, "플레이리스트에 대한 권한이 없습니다"),
    PLAYLIST_TRACK_NOT_FOUND(HttpStatus.NOT_FOUND, "플레이리스트 트랙을 찾을 수 없습니다"),
    PLAYLIST_TRACK_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 플레이리스트에 추가된 트랙입니다"),
    INVALID_PLAYLIST_TRACK_POSITION(HttpStatus.BAD_REQUEST, "유효하지 않은 플레이리스트 트랙 순서입니다"),
    INVALID_POSITION(HttpStatus.BAD_REQUEST, "유효하지 않은 플레이리스트 순서 값입니다"),
    PLAYLIST_TRACK_POSITION_CONFLICT(HttpStatus.CONFLICT, "트랙 순서 충돌이 발생했습니다");

    private final HttpStatus httpStatus;
    private final String message;
}
