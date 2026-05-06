package com.fivefy.ai.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum EmbeddingErrorCode implements ErrorCode {

    ERR_EMBEDDING_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "임베딩 서비스를 사용할 수 없습니다"),
    ERR_EMBEDDING_BATCH_SIZE_MISMATCH(HttpStatus.INTERNAL_SERVER_ERROR, "배치 처리 데이터의 개수가 일치하지 않습니다");

    private final HttpStatus httpStatus;
    private final String message;
}
