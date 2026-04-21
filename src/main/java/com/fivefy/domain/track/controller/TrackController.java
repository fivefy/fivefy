package com.fivefy.domain.track.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.domain.track.dto.response.ArtistFreeCreationTrackResponse;
import com.fivefy.domain.track.dto.response.PublicTrackListResponse;
import com.fivefy.domain.track.dto.response.TrackDetailResponse;
import com.fivefy.domain.track.service.TrackService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    /**
     * 공개 트랙 목록 조회 API
     */
    @GetMapping("/tracks")
    public ResponseEntity<BaseResponse<PageResponse<PublicTrackListResponse>>> getPublicTracks(
            @PageableDefault(
                    size = 20,
                    sort = "publishedAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {

        // 공개 트랙 목록 조회
        PageResponse<PublicTrackListResponse> response =
                trackService.getPublicTracks(pageable);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK, "공개 트랙 목록 조회 성공", response));
    }

    /**
     * 아티스트별 자유 창작 트랙 목록 조회 API
     */
    @GetMapping("/artists/{artistId}/free-creations")
    public ResponseEntity<BaseResponse<PageResponse<ArtistFreeCreationTrackResponse>>> getArtistFreeCreations(
            @PathVariable Long artistId,
            @PageableDefault(
                    size = 20,
                    sort = "publishedAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {

        // 아티스트별 자유 창작 트랙 목록 조회
        PageResponse<ArtistFreeCreationTrackResponse> response =
                trackService.getArtistFreeCreations(artistId, pageable);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK, "아티스트별 자유 창작 트랙 목록 조회 성공", response));
    }
}