package com.fivefy.domain.playlisttrack.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.domain.playlisttrack.dto.request.PlaylistTrackCreateRequest;
import com.fivefy.domain.playlisttrack.dto.request.PlaylistTrackOrderUpdateRequest;
import com.fivefy.domain.playlisttrack.dto.response.PlaylistTrackResponse;
import com.fivefy.domain.playlisttrack.service.PlaylistTrackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/playlists/{playlistId}/tracks")
public class PlaylistTrackController {

    private final PlaylistTrackService playlistTrackService;

    @PostMapping
    public ResponseEntity<BaseResponse<PlaylistTrackResponse>> addTrack(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long playlistId,
            @Valid @RequestBody PlaylistTrackCreateRequest request
    ) {
        PlaylistTrackResponse response = playlistTrackService.addTrack(userId, playlistId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(HttpStatus.CREATED, "플레이리스트 트랙 추가 성공", response));
    }

    @GetMapping
    public ResponseEntity<BaseResponse<List<PlaylistTrackResponse>>> getTracks(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long playlistId
    ) {
        List<PlaylistTrackResponse> response = playlistTrackService.getTracks(userId, playlistId);

        return ResponseEntity.ok(
                BaseResponse.success(HttpStatus.OK, "플레이리스트 트랙 목록 조회 성공", response)
        );
    }

    @PatchMapping("/index")
    public ResponseEntity<BaseResponse<Void>> updateTrackOrder(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long playlistId,
            @Valid @RequestBody PlaylistTrackOrderUpdateRequest request
    ) {
        playlistTrackService.updateTrackOrder(userId, playlistId, request);

        return ResponseEntity.ok(
                BaseResponse.success(HttpStatus.OK, "플레이리스트 트랙 순서 변경 성공", null)
        );
    }

    @DeleteMapping("/{trackId}")
    public ResponseEntity<BaseResponse<Void>> deleteTrack(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long playlistId,
            @PathVariable Long trackId
    ) {
        playlistTrackService.deleteTrack(userId, playlistId, trackId);

        return ResponseEntity.ok(
                BaseResponse.success(HttpStatus.OK, "플레이리스트 트랙 삭제 성공", null)
        );
    }
}
