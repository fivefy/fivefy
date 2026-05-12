package com.fivefy.domain.track.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum TrackApplicationErrorCode implements ErrorCode {
    ERR_TRACK_APPLICATION_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "이미 처리된 트랙 등록 신청입니다"),
    ERR_TRACK_APPLICATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 처리 중인 트랙 등록 신청이 존재합니다"),
    ERR_INVALID_PUBLISH_DELAY_DAYS(HttpStatus.BAD_REQUEST, "공개 예약 옵션은 0 이상 7 이하여야 합니다"),
    ERR_INVALID_AUDIO_FILE(HttpStatus.BAD_REQUEST, "MP3 오디오 파일은 필수입니다"),
    ERR_INACTIVE_ARTIST_CANNOT_REQUEST_OFFICIAL_RELEASE(HttpStatus.FORBIDDEN, "승인된 아티스트만 정식 발매 트랙 등록 신청이 가능합니다"),
    ERR_TRACK_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "트랙 등록 신청이 존재하지 않습니다"),
    ERR_TRACK_APPLICATION_DETAIL_FORBIDDEN(HttpStatus.FORBIDDEN, "본인 또는 관리자만 트랙 등록 신청 상세 정보를 조회할 수 있습니다"),
    ERR_ALBUM_ARTIST_MISMATCH(HttpStatus.BAD_REQUEST, "앨범과 아티스트 정보가 일치하지 않습니다");

    private final HttpStatus httpStatus;
    private final String message;

    TrackApplicationErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
