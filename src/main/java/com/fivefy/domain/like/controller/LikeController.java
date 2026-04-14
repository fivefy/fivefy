package com.fivefy.domain.like.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.domain.like.dto.request.LikeCreateRequest;
import com.fivefy.domain.like.dto.response.LikeCreateResponse;
import com.fivefy.domain.like.dto.response.LikeGetResponse;
import com.fivefy.domain.like.service.LikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api")
@RestController
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/likes")
    public ResponseEntity<BaseResponse<LikeCreateResponse>> createLike(
            @Valid @RequestBody LikeCreateRequest request,
            @AuthenticationPrincipal Long userId) {
        LikeCreateResponse response = likeService.createLike(
                request.targetId(), request.targetType(), userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(HttpStatus.CREATED, "좋아요 등록 성공", response));
    }

    @GetMapping("/likes")
    public ResponseEntity<BaseResponse<PageResponse<LikeGetResponse>>> getAllLike(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<LikeGetResponse> responses = likeService.getLikes(userId, pageable);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK, "좋아요 목록 조회 성공", PageResponse.from(responses)));
    }
}
