package com.fivefy.ai.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCode {

    ERR_AI_PLAYLIST_INVALID_INPUT(HttpStatus.BAD_REQUEST, "prompt 또는 seedTrackIds 중 하나는 필수입니다"),
    ERR_AI_PLAYLIST_SEED_NOT_FOUND(HttpStatus.NOT_FOUND, "선택한 시드 곡의 임베딩 정보를 찾을 수 없습니다");

    private final HttpStatus httpStatus;
    private final String message;
}
