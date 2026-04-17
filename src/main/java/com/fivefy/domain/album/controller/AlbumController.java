package com.fivefy.domain.album.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.album.dto.request.AlbumApplicationCreateRequest;
import com.fivefy.domain.album.dto.request.AlbumApplicationRejectRequest;
import com.fivefy.domain.album.dto.response.*;
import com.fivefy.domain.album.service.AlbumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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
     * 앨범 등록 신청 생성 API
     */
    @PostMapping("/album-applications")
    public ResponseEntity<BaseResponse<AlbumApplicationResponse>> createAlbumApplication(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AlbumApplicationCreateRequest request
    ) {
        AlbumApplicationResponse response =
                albumService.createAlbumApplication(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                BaseResponse.success(HttpStatus.CREATED, "앨범 등록 신청 성공", response)
        );
    }

    /**
     * 내 앨범 등록 신청 목록 조회 API
     */
    @GetMapping("/album-applications/me")
    public ResponseEntity<BaseResponse<List<AlbumApplicationResponse>>> getMyAlbumApplications(
            @AuthenticationPrincipal Long userId
    ) {
        List<AlbumApplicationResponse> response =
                albumService.getMyAlbumApplications(userId);

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "내 앨범 등록 신청 목록 조회 성공", response)
        );
    }

    /**
     * 앨범 등록 신청 상세 조회 API
     */
    @GetMapping("/album-applications/{applicationId}")
    public ResponseEntity<BaseResponse<AlbumApplicationDetailResponse>> getAlbumApplication(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long applicationId
    ) {
        AlbumApplicationDetailResponse response =
                albumService.getAlbumApplication(userId, applicationId);

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "앨범 등록 신청 상세 조회 성공", response)
        );
    }

    /**
     * 앨범 등록 신청 목록 조회 API
     */
    @GetMapping("/admin/album-applications")
    public ResponseEntity<BaseResponse<PageResponse<AlbumApplicationListResponse>>> getAlbumApplications(
            @RequestParam(required = false) ApplicationStatus status,
            Pageable pageable
    ) {
        PageResponse<AlbumApplicationListResponse> response =
                albumService.getAlbumApplications(status, pageable);

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "앨범 등록 신청 목록 조회 성공", response)
        );
    }

    /**
     * 앨범 등록 신청 승인 API
     */
    @PostMapping("/admin/album-applications/{applicationId}/approve")
    public ResponseEntity<BaseResponse<AlbumApplicationApproveResponse>> approveAlbumApplication(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long applicationId
    ) {
        AlbumApplicationApproveResponse response =
                albumService.approveAlbumApplication(adminId, applicationId);

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "앨범 등록 신청 승인 성공", response)
        );
    }

    /**
     * 앨범 등록 신청 거절 API
     */
    @PostMapping("/admin/album-applications/{applicationId}/reject")
    public ResponseEntity<BaseResponse<AlbumApplicationRejectResponse>> rejectAlbumApplication(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long applicationId,
            @Valid @RequestBody AlbumApplicationRejectRequest request
    ) {
        AlbumApplicationRejectResponse response =
                albumService.rejectAlbumApplication(adminId, applicationId, request.rejectionReason());

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "앨범 등록 신청 거절 성공", response)
        );
    }
}