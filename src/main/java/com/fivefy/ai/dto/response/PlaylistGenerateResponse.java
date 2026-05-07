package com.fivefy.ai.dto.response;

import java.util.List;

public record PlaylistGenerateResponse(
        String searchTextUsed,                  // 디버깅용 - LLM이 만든 검색 텍스트
        List<RecommendationResponse.RecommendedTrack> tracks
) {
}
