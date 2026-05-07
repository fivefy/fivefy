package com.fivefy.ai.dto.response;

import java.util.List;

public record MoodSearchResponse(
        String searchTextUsed,           // LLM이 만든 검색 텍스트 (디버깅)
        String modeUsed,                 // 적용된 모드
        List<MoodTrack> tracks
) {
}
