package com.fivefy.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BaseResponse<T>(
        boolean success,
        HttpStatus status,
        String message,
        T data
) {

    // 성공 응답
    public static <T> BaseResponse<T> success(HttpStatus status, String message, T data) {
        return new BaseResponse<>(true, status, message, data);
    }

    // 실패 응답 - data가 null인 에러 응답
    public static <T> BaseResponse<T> fail(HttpStatus status, String message) {
        return new BaseResponse<>(false, status, message, null);
    }

    // 실패 응답 - validation으로 여러 오류 메세지 발생 시 data 이용하여 응답
    public static <T> BaseResponse<T> fail(HttpStatus status, String message, T data) {
        return new BaseResponse<>(false, status, message, data);
    }
}
