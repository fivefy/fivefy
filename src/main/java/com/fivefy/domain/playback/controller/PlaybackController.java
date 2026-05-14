package com.fivefy.domain.playback.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.domain.playback.dto.request.PlaybackPauseRequest;
import com.fivefy.domain.playback.dto.request.PlaybackPlayRequest;
import com.fivefy.domain.playback.dto.request.PlaybackSkipRequest;
import com.fivefy.domain.playback.dto.request.PlaybackStopRequest;
import com.fivefy.domain.playback.dto.response.PlaybackResponse;
import com.fivefy.domain.playback.dto.response.TrackPlayResponse;
import com.fivefy.domain.playback.service.PlaybackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PlaybackController {

    private final PlaybackService playbackService;

    @PostMapping("/tracks/{trackId}/play")
    public ResponseEntity<BaseResponse<TrackPlayResponse>> playTrack(
            @PathVariable Long trackId
    ) {
        TrackPlayResponse response = playbackService.playTrack(trackId);

        return ResponseEntity.ok(
                BaseResponse.success(HttpStatus.OK, "트랙 재생 URL 발급 성공", response)
        );
    }

    @PostMapping("/playbacks/play")
    public ResponseEntity<BaseResponse<PlaybackResponse>> play(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PlaybackPlayRequest request
    ) {
        PlaybackResponse response = playbackService.play(userId, request);

        return ResponseEntity.ok(
                BaseResponse.success(HttpStatus.OK, "재생 시작 성공", response)
        );
    }

    @PostMapping("/playbacks/pause")
    public ResponseEntity<BaseResponse<PlaybackResponse>> pause(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PlaybackPauseRequest request
    ) {
        PlaybackResponse response = playbackService.pause(userId, request);

        return ResponseEntity.ok(
                BaseResponse.success(HttpStatus.OK, "재생 일시정지 성공", response)
        );
    }

    @PostMapping("/playbacks/stop")
    public ResponseEntity<BaseResponse<PlaybackResponse>> stop(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PlaybackStopRequest request
    ) {
        PlaybackResponse response = playbackService.stop(userId, request);

        return ResponseEntity.ok(
                BaseResponse.success(HttpStatus.OK, "곡 정지 성공", response)
        );
    }

    @PostMapping("/playbacks/skip")
    public ResponseEntity<BaseResponse<PlaybackResponse>> skip(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PlaybackSkipRequest request
    ) {
        PlaybackResponse response = playbackService.skip(userId, request);

        return ResponseEntity.ok(
                BaseResponse.success(HttpStatus.OK, "곡 건너뛰기 성공", response)
        );
    }

    @GetMapping("/me/playback-history")
    public ResponseEntity<BaseResponse<List<PlaybackResponse>>> getPlaybackHistory(
            @AuthenticationPrincipal Long userId
    ) {
        List<PlaybackResponse> response = playbackService.getPlaybackHistory(userId);

        return ResponseEntity.ok(
                BaseResponse.success(HttpStatus.OK, "재생 기록 조회 성공", response)
        );
    }
}
