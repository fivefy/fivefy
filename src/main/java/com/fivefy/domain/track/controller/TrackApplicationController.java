package com.fivefy.domain.track.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.track.dto.request.FreeTrackApplicationCreateRequest;
import com.fivefy.domain.track.dto.request.OfficialTrackApplicationCreateRequest;
import com.fivefy.domain.track.dto.response.TrackApplicationApproveResponse;
import com.fivefy.domain.track.dto.response.TrackApplicationDetailResponse;
import com.fivefy.domain.track.dto.response.TrackApplicationListResponse;
import com.fivefy.domain.track.dto.response.TrackApplicationResponse;
import com.fivefy.domain.track.service.TrackService;
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

/**
 * 트랙 등록 신청 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TrackApplicationController {

    private final TrackService trackService;

    /**
     * 자유 창작 트랙 등록 신청 API
     */
    @PostMapping("/track-applications/free-creations")
    public ResponseEntity<BaseResponse<TrackApplicationResponse>> createFreeTrackApplication(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FreeTrackApplicationCreateRequest request
    ) {
        TrackApplicationResponse response =
                trackService.createFreeTrackApplication(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                BaseResponse.success(HttpStatus.CREATED, "자유 창작 트랙 등록 신청 성공", response)
        );
    }

    /**
     * 정식 발매 트랙 등록 신청 API
     */
    @PostMapping("/track-applications/official-releases")
    public ResponseEntity<BaseResponse<TrackApplicationResponse>> createOfficialTrackApplication(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OfficialTrackApplicationCreateRequest request
    ) {
        TrackApplicationResponse response =
                trackService.createOfficialTrackApplication(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                BaseResponse.success(HttpStatus.CREATED, "정식 발매 트랙 등록 신청 성공", response)
        );
    }

    /**
     * 내 트랙 등록 신청 목록 조회 API
     */
    @GetMapping("/track-applications/me")
    public ResponseEntity<BaseResponse<List<TrackApplicationResponse>>> getMyTrackApplications(
            @AuthenticationPrincipal Long userId
    ) {
        List<TrackApplicationResponse> response =
                trackService.getMyTrackApplications(userId);

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "내 트랙 등록 신청 목록 조회 성공", response)
        );
    }

    /**
     * 트랙 등록 신청 상세 조회 API
     */
    @GetMapping("/track-applications/{applicationId}")
    public ResponseEntity<BaseResponse<TrackApplicationDetailResponse>> getTrackApplication(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long applicationId
    ) {
        TrackApplicationDetailResponse response =
                trackService.getTrackApplication(userId, applicationId);

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "트랙 등록 신청 상세 조회 성공", response)
        );
    }

    /**
     * 트랙 등록 신청 목록 조회 API (관리자)
     */
    @GetMapping("/admin/track-applications")
    public ResponseEntity<BaseResponse<PageResponse<TrackApplicationListResponse>>> getTrackApplications(
            @RequestParam(required = false) ApplicationStatus status,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        PageResponse<TrackApplicationListResponse> response =
                trackService.getTrackApplications(status, pageable);

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "트랙 등록 신청 목록 조회 성공", response)
        );
    }

    /**
     * 트랙 등록 신청 승인 API (관리자)
     */
    @PostMapping("/admin/track-applications/{applicationId}/approve")
    public ResponseEntity<BaseResponse<TrackApplicationApproveResponse>> approveTrackApplication(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long applicationId
    ) {
        TrackApplicationApproveResponse response =
                trackService.approveTrackApplication(adminId, applicationId);

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "트랙 등록 신청 승인 성공", response)
        );
    }
}