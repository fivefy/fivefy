package com.fivefy.ai.controller;

import com.fivefy.ai.dto.PlaylistGenerateRequest;
import com.fivefy.ai.dto.PlaylistGenerateResponse;
import com.fivefy.ai.service.PlaylistGenerationService;
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
@RequestMapping("/api/ai/playlists")
@RequiredArgsConstructor
public class PlaylistGenerationController {

    private final PlaylistGenerationService playlistGenerationService;

    /**
     * POST /api/playlists/generate
     *
     * 요청 예시:
     * {
     *   "prompt": "비 오는 일요일 오후에 듣기 좋은 곡",
     *   "seedTrackIds": [42],
     *   "size": 20,
     *   "diversityLambda": 0.5
     * }
     *
     * 응답 예시:
     * {
     *   "name": null,
     *   "description": null,
     *   "searchTextUsed": "Mellow indie folk, acoustic guitar, ...",
     *   "tracks": [
     *     { "trackId": 142, "title": "...", "artist": "...", "relevanceScore": 0.83 },
     *     ...
     *   ]
     * }
     */
    @PostMapping("/generate")
    public ResponseEntity<BaseResponse<PlaylistGenerateResponse>> generate(
            @Valid @RequestBody PlaylistGenerateRequest request) {

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "플레이리스트 자동 생성 성공", playlistGenerationService.generate(request))
        );
    }
}
