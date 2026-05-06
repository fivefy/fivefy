package com.fivefy.ai.dto.response;

import java.util.List;

public record RecommendationResponse(
        List<RecommendedTrack> tracks,
        String reasoningHint,    // "최근 들으신 K-pop 발라드 기반" 등
        int basedOnCount         // 추천 산정에 쓰인 청취 곡 수 (신뢰도 지표)
) {
    public record RecommendedTrack(
            Long trackId,
            String title,
            String artist,
            String albumCoverUrl,
            float relevanceScore  // 0-1, UI에서 정렬/필터에 사용
    ) {}
}
