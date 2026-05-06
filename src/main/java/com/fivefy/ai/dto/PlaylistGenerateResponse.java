package com.fivefy.ai.dto;

import java.util.List;

public record PlaylistGenerateResponse(
        String name,                            // LLM이 생성한 플레이리스트 이름 (선택)
        String description,                     // LLM이 생성한 설명 (선택)
        String searchTextUsed,                  // 디버깅용 - LLM이 만든 검색 텍스트
        List<RecommendationResponse.RecommendedTrack> tracks
) {
}
