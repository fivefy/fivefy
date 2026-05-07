package com.fivefy.ai.dto.request;

import com.fivefy.ai.enums.MoodSearchMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MoodSearchRequest(
        @NotBlank(message = "검색어는 필수입니다")
        @Size(max = 200, message = "검색어는 최대 200자 까지 가능합니다")
        String query,

        @Min(1) @Max(50)
        int limit,

        MoodSearchMode mode
) {
    public MoodSearchRequest {
        if (mode == null) mode = MoodSearchMode.BALANCED;
    }

    public double alpha() {
        return switch (mode) {
            case LYRICS   -> 0.3;
            case BALANCED -> 0.6;
            case METADATA -> 0.9;
        };
    }
}
