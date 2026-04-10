package com.fivefy.common.util;

import java.util.Objects;

public class ValidationUtils {

    private static void validateNotNull(Object value, String fieldName) {
        Objects.requireNonNull(value, fieldName + "는 필수입니다. (validationNotNull)");
    }
}
