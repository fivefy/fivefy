package com.fivefy.domain.like.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.domain.like.dto.request.LikeCreateRequest;
import com.fivefy.domain.like.dto.response.LikeCreateResponse;
import com.fivefy.domain.like.service.LikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
