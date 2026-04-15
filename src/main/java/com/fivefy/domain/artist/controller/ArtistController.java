package com.fivefy.domain.artist.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.domain.artist.dto.request.ArtistApplicationCreateRequest;
import com.fivefy.domain.artist.dto.response.ArtistApplicationDetailResponse;
import com.fivefy.domain.artist.dto.response.ArtistApplicationListResponse;
import com.fivefy.domain.artist.dto.response.ArtistApplicationResponse;
import com.fivefy.domain.artist.service.ArtistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ArtistController {

    private final ArtistService artistService;

    /**
     * 아티스트 등록 요청 생성 API
     */
    @PostMapping("/artist-applications")
    public ResponseEntity<BaseResponse<ArtistApplicationResponse>> createArtistApplication(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ArtistApplicationCreateRequest request
    ) {
        ArtistApplicationResponse response = artistService.createArtistApplication(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                BaseResponse.success(HttpStatus.CREATED, "아티스트 등록 요청 성공", response)
        );
    }

    /**
     * 내 아티스트 등록 요청 목록 조회 API
     */
    @GetMapping("/artist-applications/me")
    public ResponseEntity<BaseResponse<List<ArtistApplicationResponse>>> getMyArtistApplications(
            @AuthenticationPrincipal Long userId
    ) {
        List<ArtistApplicationResponse> response = artistService.getMyArtistApplications(userId);

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "내 아티스트 등록 요청 목록 조회 성공", response)
        );
    }

    /**
     * 관리자용 아티스트 등록 요청 목록 조회 API
     */
    @GetMapping("/admin/artist-applications")
    public ResponseEntity<BaseResponse<PageResponse<ArtistApplicationListResponse>>> getArtistApplications(
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        PageResponse<ArtistApplicationListResponse> response =
                artistService.getArtistApplications(pageable);

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "아티스트 등록 요청 목록 조회 성공", response));
    }

    /**
     * 아티스트 등록 요청 목록 상세 조회 API
     */
    @GetMapping("/artist-applications/{applicationId}")
    public ResponseEntity<BaseResponse<ArtistApplicationDetailResponse>> getArtistApplication(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long applicationId
    ) {
        ArtistApplicationDetailResponse response =
                artistService.getArtistApplication(userId, applicationId);

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "아티스트 등록 요청 상세 조회 성공", response));
    }
}