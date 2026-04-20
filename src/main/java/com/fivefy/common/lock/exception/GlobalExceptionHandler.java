package com.fivefy.common.lock.exception;

import com.fivefy.common.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiResponse> handleServiceException(ServiceException e) {

        log.warn("ServiceException 발생: ", e);
        HttpStatus httpStatus = e.getErrorEnum().getHttpStatus();
        String error = e.getMessage();

        return ResponseEntity.status(httpStatus).body(ApiResponse.fail(String.valueOf(httpStatus.value()), error));
    }
}