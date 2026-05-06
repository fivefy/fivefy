package com.fivefy.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PlaylistGenerateRequest(
        String prompt,                          // 자연어 분위기 ("비 오는 일요일")
        @Size(max = 5)
        List<Long> seedTrackIds,                // 시드 트랙 (최대 5개)
        @Min(5) @Max(50)
        int size,                               // 생성할 곡 수
        Double diversityLambda                  // MMR lambda (없으면 기본값)
) {
    public boolean hasInput() {
        return (prompt != null && !prompt.isBlank())
                || (seedTrackIds != null && !seedTrackIds.isEmpty());
    }
}
