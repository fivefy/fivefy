package com.fivefy.common.util;

public class ValidationUtils {

    public static void validateNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "은/는 필수입니다.");
        }
    }
}
