package com.fivefy.ai.controller;

import com.fivefy.ai.dto.MoodSearchRequest;
import com.fivefy.ai.dto.MoodSearchResponse;
import com.fivefy.ai.service.MoodSearchService;
import com.fivefy.common.dto.response.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/search/mood")
@RequiredArgsConstructor
public class MoodSearchController {

    private final MoodSearchService moodSearchService;

    /**
     * POST /api/ai/search/mood
     *
     * 요청:
     * {
     *   "query": "이별 후에 혼자 우는 곡",
     *   "limit": 20,
     *   "mode": "LYRICS"
     * }
     *
     * 응답:
     * {
     *   "searchTextUsed": "Heartbreak ballad, slow piano, ...",
     *   "modeUsed": "LYRICS",
     *   "tracks": [
     *     {
     *       "trackId": 142,
     *       "title": "...",
     *       "finalScore": 0.87,
     *       "metaScore": 0.72,
     *       "lyricsScore": 0.95,
     *       "hasLyrics": true
     *     }
     *   ]
     * }
     */
    @PostMapping
    public ResponseEntity<BaseResponse<MoodSearchResponse>> search(@Valid @RequestBody MoodSearchRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "무드 검색 성공", moodSearchService.search(request))
        );
    }
}
