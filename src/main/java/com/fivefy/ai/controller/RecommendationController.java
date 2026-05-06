package com.fivefy.ai.controller;

import com.fivefy.ai.dto.response.RecommendationResponse;
import com.fivefy.ai.service.RecommendationService;
import com.fivefy.common.dto.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<BaseResponse<RecommendationResponse>> recommend(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "20") int limit) {

        int safeLimit = Math.min(Math.max(limit, 1), 100);

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "곡 추천 성공", recommendationService.recommendForUser(userId, safeLimit))
        );
    }
}
