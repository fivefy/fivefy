package com.fivefy.common.util;

import java.util.Objects;

public class ValidationUtils {

    public static void validateNonNull(Object value, String fieldName) {
        Objects.requireNonNull(value, fieldName + "(은)는 필수입니다");
    }
}
