package com.fivefy.common.exception;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.common.enums.MultipartErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Nested
    @DisplayName("multipart 업로드 예외 처리")
    class HandleMultipartException {

        @Test
        @DisplayName("업로드 용량 초과 시 413을 반환한다")
        void handleMaxUploadSizeExceededException_success() {
            ResponseEntity<BaseResponse<Void>> response = exceptionHandler.handleMaxUploadSizeExceededException(
                    new MaxUploadSizeExceededException(50 * 1024 * 1024)
            );

            assertThat(response.getStatusCode()).isEqualTo(MultipartErrorCode.ERR_MULTIPART_UPLOAD_SIZE_EXCEEDED.getHttpStatus());
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().success()).isFalse();
            assertThat(response.getBody().status()).isEqualTo(MultipartErrorCode.ERR_MULTIPART_UPLOAD_SIZE_EXCEEDED.getHttpStatus());
            assertThat(response.getBody().message()).isEqualTo(MultipartErrorCode.ERR_MULTIPART_UPLOAD_SIZE_EXCEEDED.getMessage());
        }

        @Test
        @DisplayName("multipart 요청 형식이 잘못되면 400을 반환한다")
        void handleMultipartException_success() {
            ResponseEntity<BaseResponse<Void>> response = exceptionHandler.handleMultipartException(
                    new MultipartException("multipart 요청 형식 오류")
            );

            assertThat(response.getStatusCode()).isEqualTo(MultipartErrorCode.ERR_INVALID_MULTIPART_REQUEST.getHttpStatus());
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().success()).isFalse();
            assertThat(response.getBody().status()).isEqualTo(MultipartErrorCode.ERR_INVALID_MULTIPART_REQUEST.getHttpStatus());
            assertThat(response.getBody().message()).isEqualTo(MultipartErrorCode.ERR_INVALID_MULTIPART_REQUEST.getMessage());
        }
    }
}
