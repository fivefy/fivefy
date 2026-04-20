package com.fivefy.domain.track.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.domain.track.dto.response.TrackDetailResponse;
import com.fivefy.domain.track.service.TrackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 트랙 도메인 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TrackController {

    private final TrackService trackService;

    /**
     * 트랙 상세 조회 API
     */
    @GetMapping("/tracks/{trackId}")
    public ResponseEntity<BaseResponse<TrackDetailResponse>> getTrack(
            @PathVariable Long trackId
    ) {
        TrackDetailResponse response = trackService.getTrack(trackId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK, "트랙 상세 조회 성공", response));
    }
}