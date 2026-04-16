package com.fivefy.domain.album.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.domain.album.dto.request.AlbumReleaseRequestCreateRequest;
import com.fivefy.domain.album.dto.response.AlbumReleaseRequestDetailResponse;
import com.fivefy.domain.album.dto.response.AlbumReleaseRequestResponse;
import com.fivefy.domain.album.service.AlbumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 앨범 도메인 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AlbumController {

    private final AlbumService albumService;

    /**
     * 앨범 등록 요청 생성 API
     */
    @PostMapping("/album-release-requests")
    public ResponseEntity<BaseResponse<AlbumReleaseRequestResponse>> createAlbumReleaseRequest(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AlbumReleaseRequestCreateRequest request
    ) {
        AlbumReleaseRequestResponse response =
                albumService.createAlbumReleaseRequest(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                BaseResponse.success(HttpStatus.CREATED, "앨범 등록 요청 성공", response)
        );
    }

    /**
     * 내 앨범 등록 요청 목록 조회 API
     */
    @GetMapping("/album-release-requests/me")
    public ResponseEntity<BaseResponse<List<AlbumReleaseRequestResponse>>> getMyAlbumReleaseRequests(
            @AuthenticationPrincipal Long userId
    ) {
        List<AlbumReleaseRequestResponse> response =
                albumService.getMyAlbumReleaseRequests(userId);

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "내 앨범 등록 요청 목록 조회 성공", response)
        );
    }

    /**
     * 앨범 등록 요청 상세 조회 API
     */
    @GetMapping("/album-release-requests/{requestId}")
    public ResponseEntity<BaseResponse<AlbumReleaseRequestDetailResponse>> getAlbumReleaseRequest(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long requestId
    ) {
        AlbumReleaseRequestDetailResponse response =
                albumService.getAlbumReleaseRequest(userId, requestId);

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "앨범 등록 요청 상세 조회 성공", response)
        );
    }
}