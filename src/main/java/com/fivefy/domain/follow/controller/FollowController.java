package com.fivefy.domain.follow.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.domain.follow.dto.request.FollowCreateRequest;
import com.fivefy.domain.follow.dto.response.FollowCreateResponse;
import com.fivefy.domain.follow.dto.response.FollowGetResponse;
import com.fivefy.domain.follow.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api")
@RestController
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/follows")
    public ResponseEntity<BaseResponse<FollowCreateResponse>> createFollow (
            @AuthenticationPrincipal Long userId,
            @RequestBody FollowCreateRequest request) {
        FollowCreateResponse response = followService.createFollow(userId, request.artistId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(HttpStatus.CREATED, "팔로우 등록 성공", response));
    }

    @GetMapping("/follows")
    public ResponseEntity<BaseResponse<List<FollowGetResponse>>> getAllFollow(
            @AuthenticationPrincipal Long userId) {
        List<FollowGetResponse> responses = followService.getFollows(userId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK, "팔로우 목록 조회 성공", responses));
    }

    @DeleteMapping("/follows/{followId}")
    public ResponseEntity<BaseResponse<Void>>  deleteFollow(
            @AuthenticationPrincipal Long userId, @PathVariable Long followId) {
        followService.deleteFollow(userId, followId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK, "팔로우 취소 성공", null));
    }

    @PatchMapping("/follows/{followId}/notifications")
    public ResponseEntity<BaseResponse<Void>> toggleNotification(
            @AuthenticationPrincipal Long userId, @PathVariable Long followId) {
        followService.toggleNotification(userId, followId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK, "알림 설정 변경 성공", null));
    }
}
