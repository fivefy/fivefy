package com.fivefy.domain.follow.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.domain.follow.dto.request.FollowCreateRequest;
import com.fivefy.domain.follow.dto.response.FollowCreateResponse;
import com.fivefy.domain.follow.dto.response.FollowGetResponse;
import com.fivefy.domain.follow.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api")
@RestController
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/follows")
    public ResponseEntity<BaseResponse<FollowCreateResponse>> createFollow (@RequestBody FollowCreateRequest request) {
        FollowCreateResponse response = followService.createFollow(request.userId(), request.artistId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(HttpStatus.CREATED, "팔로우 등록 성공", response));
    }

    @GetMapping("/follows")
    public ResponseEntity<BaseResponse<List<FollowGetResponse>>> getAllFollow(@RequestParam Long userId) {
        List<FollowGetResponse> responses = followService.getFollows(userId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK, "팔로우 목록 조회 성공", responses));
    }
}
