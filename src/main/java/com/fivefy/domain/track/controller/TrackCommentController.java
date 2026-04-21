package com.fivefy.domain.track.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.domain.track.dto.request.TrackCommentCreateRequest;
import com.fivefy.domain.track.dto.response.TrackCommentResponse;
import com.fivefy.domain.track.service.TrackCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 트랙 댓글 도메인 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TrackCommentController {

    private final TrackCommentService trackCommentService;

    /**
     * 트랙 댓글 작성 API
     */
    @PostMapping("/tracks/{trackId}/comments")
    public ResponseEntity<BaseResponse<TrackCommentResponse>> createTrackComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long trackId,
            @Valid @RequestBody TrackCommentCreateRequest request
    ) {
        TrackCommentResponse response =
                trackCommentService.createTrackComment(userId, trackId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                BaseResponse.success(HttpStatus.CREATED, "트랙 댓글 작성 성공", response)
        );
    }

    /**
     * 트랙 댓글 목록 조회 API
     */
    @GetMapping("/tracks/{trackId}/comments")
    public ResponseEntity<BaseResponse<PageResponse<TrackCommentResponse>>> getTrackComments(
            @PathVariable Long trackId,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        PageResponse<TrackCommentResponse> response =
                trackCommentService.getTrackComments(trackId, pageable);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK, "트랙 댓글 목록 조회 성공", response));
    }
}