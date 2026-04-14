package com.fivefy.common.util;

import java.util.Objects;

public class ValidationUtils {

    public static void validateNonNull(Object value, String fieldName) {
        Objects.requireNonNull(value, fieldName + "(은)는 필수입니다");
    }

    public static void validatePositive(Long value, String fieldName) {
        validateNonNull(value, fieldName);
        if (value <= 0) {
            throw new NullPointerException(fieldName + "(은)는 0보다 커야 합니다");
        }
    }
}
