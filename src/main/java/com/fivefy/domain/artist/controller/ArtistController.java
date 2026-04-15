package com.fivefy.domain.artist.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.artist.dto.request.ArtistApplicationCreateRequest;
import com.fivefy.domain.artist.dto.request.ArtistApplicationRejectRequest;
import com.fivefy.domain.artist.dto.request.ArtistProfileUpdateRequest;
import com.fivefy.domain.artist.dto.response.*;
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
            @RequestParam(required = false) ApplicationStatus status,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        PageResponse<ArtistApplicationListResponse> response =
                artistService.getArtistApplications(status, pageable);

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "아티스트 등록 요청 목록 조회 성공", response)
        );
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
                BaseResponse.success(HttpStatus.OK, "아티스트 등록 요청 상세 조회 성공", response)
        );
    }

    /**
     * 아티스트 등록 요청 승인 API
     */
    @PostMapping("/admin/artist-applications/{applicationId}/approve")
    public ResponseEntity<BaseResponse<ArtistApplicationApproveResponse>> approveArtistApplication(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long applicationId
    ) {
        ArtistApplicationApproveResponse response =
                artistService.approveArtistApplication(userId, applicationId);

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "아티스트 등록 요청 승인 성공", response)
        );
    }

    /**
     * 아티스트 등록 요청 거절 API
     */
    @PostMapping("/admin/artist-applications/{applicationId}/reject")
    public ResponseEntity<BaseResponse<ArtistApplicationRejectResponse>> rejectArtistApplication(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long applicationId,
            @Valid @RequestBody ArtistApplicationRejectRequest request
    ) {
        ArtistApplicationRejectResponse response =
                artistService.rejectArtistApplication(userId, applicationId, request);

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "아티스트 등록 요청 거절 성공", response)
        );
    }

    /**
     * 내 아티스트 목록 조회 API
     */
    @GetMapping("/my/artists")
    public ResponseEntity<BaseResponse<List<MyArtistResponse>>> getMyArtists(
            @AuthenticationPrincipal Long userId
    ) {
        List<MyArtistResponse> response = artistService.getMyArtists(userId);

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "내 아티스트 목록 조회 성공", response)
        );
    }

    /**
     * 아티스트 상세 조회 API
     */
    @GetMapping("/artists/{artistId}")
    public ResponseEntity<BaseResponse<ArtistDetailResponse>> getArtist(
            @PathVariable Long artistId
    ) {
        ArtistDetailResponse response = artistService.getArtist(artistId);

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "아티스트 상세 조회 성공", response)
        );
    }

    /**
     * 아티스트 프로필 수정 API
     */
    @PatchMapping("/artists/{artistId}")
    public ResponseEntity<BaseResponse<ArtistDetailResponse>> updateArtistProfile(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long artistId,
            @Valid @RequestBody ArtistProfileUpdateRequest request
    ) {
        ArtistDetailResponse response =
                artistService.updateArtistProfile(userId, artistId, request);

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "아티스트 프로필 수정 성공", response)
        );
    }

    /**
     * 아티스트 삭제 API
     */
    @DeleteMapping("/artists/{artistId}")
    public ResponseEntity<BaseResponse<Void>> deleteArtist(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long artistId
    ) {
        artistService.deleteArtist(userId, artistId);

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "아티스트 삭제 성공", null)
        );
    }

    /**
     * 아티스트 활성화 API
     */
    @PatchMapping("/artists/{artistId}/activate")
    public ResponseEntity<BaseResponse<ArtistDetailResponse>> activateArtist(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long artistId
    ) {
        ArtistDetailResponse response = artistService.activateArtist(userId, artistId);

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "아티스트 활성화 성공", response)
        );
    }
}