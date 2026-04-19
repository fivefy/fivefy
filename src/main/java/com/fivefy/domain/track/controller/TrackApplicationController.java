package com.fivefy.domain.track.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.domain.track.dto.request.FreeTrackApplicationCreateRequest;
import com.fivefy.domain.track.dto.response.TrackApplicationResponse;
import com.fivefy.domain.track.service.TrackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}