package com.fivefy.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MoodSearchRequest(
        @NotBlank(message = "검색어는 필수입니다")
        @Size(max = 200, message = "검색어는 최대 200자 까지 가능합니다")
        String query,                    // "이별 후에 혼자 우는 곡"

        @Min(1) @Max(50)
        int limit,

        /**
         * 검색 모드 — 가사:메타 가중치 비율 결정
         * - BALANCED  (기본): α=0.6
         * - LYRICS    (감정 우선): α=0.3
         * - METADATA  (장르 우선): α=0.9
         */
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
