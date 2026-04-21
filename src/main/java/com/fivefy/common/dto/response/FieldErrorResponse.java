package com.fivefy.common.dto.response;

public record FieldErrorResponse(
        String field,
        String message
) {}
