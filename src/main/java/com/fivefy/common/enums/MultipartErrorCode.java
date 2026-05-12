package com.fivefy.common.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MultipartErrorCode implements ErrorCode {
    ERR_MULTIPART_UPLOAD_SIZE_EXCEEDED(HttpStatus.CONTENT_TOO_LARGE, "업로드 가능한 파일 크기를 초과했습니다"),
    ERR_INVALID_MULTIPART_REQUEST(HttpStatus.BAD_REQUEST, "유효하지 않은 multipart 요청입니다");

    private final HttpStatus httpStatus;
    private final String message;
}
