package com.fivefy.ai.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PlaylistGenerateRequest(
        String prompt,
        @Size(max = 5, message = "시드 트랙은 최대 5개 까지 가능합니다")
        List<Long> seedTrackIds,
        @Min(5) @Max(50)
        int size,
        Double diversityLambda  // MMR lambda (없으면 기본값)
) {
    public boolean hasInput() {
        return (prompt != null && !prompt.isBlank())
                || (seedTrackIds != null && !seedTrackIds.isEmpty());
    }
}
