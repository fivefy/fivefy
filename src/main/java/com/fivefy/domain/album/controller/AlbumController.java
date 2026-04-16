package com.fivefy.domain.album.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.domain.album.dto.request.AlbumReleaseRequestCreateRequest;
import com.fivefy.domain.album.dto.response.AlbumReleaseRequestCreateResponse;
import com.fivefy.domain.album.service.AlbumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<BaseResponse<AlbumReleaseRequestCreateResponse>> createAlbumReleaseRequest(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AlbumReleaseRequestCreateRequest request
    ) {
        AlbumReleaseRequestCreateResponse response =
                albumService.createAlbumReleaseRequest(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                BaseResponse.success(HttpStatus.CREATED, "앨범 등록 요청 성공", response)
        );
    }
}