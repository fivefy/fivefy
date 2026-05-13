package com.fivefy.common.exception;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.common.dto.response.FieldErrorResponse;
import com.fivefy.common.enums.MultipartErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 여러 도메인 에러들을 관리하는 BusinessException 처리
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("비즈니스 에러 발생 : {}, 에러 상세 내용 : ", e.getMessage(), e);
        return ResponseEntity.status(e.getHttpStatus()).body(
                BaseResponse.fail(e.getHttpStatus(), e.getMessage())
        );
    }

    // validation 예외 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<List<FieldErrorResponse>>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("요청 검증 에러 발생 : ", e);
        List<FieldErrorResponse> errors = e.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                BaseResponse.fail(HttpStatus.BAD_REQUEST, "유효하지 않은 요청입니다", errors)
        );
    }

    // multipart 업로드 용량 초과 예외 처리
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<BaseResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("업로드 용량 초과 : {}", e.getMessage());
        return toErrorResponse(MultipartErrorCode.ERR_MULTIPART_UPLOAD_SIZE_EXCEEDED);
    }

    // multipart 필수 파트 누락 예외 처리
    @ExceptionHandler(MissingServletRequestPartException.class)
        public ResponseEntity<BaseResponse<Void>> handleMissingServletRequestPartException(MissingServletRequestPartException e) {
        log.warn("multipart 필수 파트 누락 : {}", e.getRequestPartName());
        return toErrorResponse(MultipartErrorCode.ERR_MISSING_MULTIPART_PART);
    }

    // multipart 요청 형식 예외 처리
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<BaseResponse<Void>> handleMultipartException(MultipartException e) {
        log.warn("multipart 요청 형식 에러 발생 : ", e);
        return toErrorResponse(MultipartErrorCode.ERR_INVALID_MULTIPART_REQUEST);
    }

    // 존재하지 않는 경로 예외 처리
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                BaseResponse.fail(HttpStatus.NOT_FOUND, "잘못된 접근입니다")
        );
    }

    // Cookie에 Refresh Token이 없을 경우 발생하는 예외 처리
    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<BaseResponse<Void>> handleMissingRequestCookieException(MissingRequestCookieException e) {
        log.warn("인증 쿠키 누락 : {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                BaseResponse.fail(HttpStatus.UNAUTHORIZED, "인증 정보가 유효하지 않습니다")
        );
    }

    // 예상치 못한 예외 전범위적 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleUnexpectedException(Exception e) {
        log.error("예상치 못한 에러 발생 : ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                BaseResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류로 인해 잠시 후 시도 바랍니다")
        );
    }

    private ResponseEntity<BaseResponse<Void>> toErrorResponse(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getHttpStatus()).body(
                BaseResponse.fail(errorCode.getHttpStatus(), errorCode.getMessage())
        );
    }
}
