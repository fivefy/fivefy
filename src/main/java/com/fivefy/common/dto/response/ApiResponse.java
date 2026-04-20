package com.fivefy.common.dto.response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@JsonPropertyOrder({"success", "data", "code", "error", "timestamp"})
public class ApiResponse<T> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final boolean success;
    private final T data;
    private final String code;
    private final String error;
    private final String timestamp;

    public static <T> ApiResponse<T> success(String code, T data) {
        return new ApiResponse<>(true, data, code, null, now());
    }

    public static <T> ApiResponse<T> fail(String code, String error) {
        return new ApiResponse<>(false, null, code, error, now());
    }

    private static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }
}