package com.fivefy.domain.subscription.controller;


import com.fivefy.domain.subscription.dto.SubscriptionResponse;
import com.fivefy.domain.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/me/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * 내 구독 조회
     * GET /api/me/subscriptions
     */
    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> getMySubscriptions(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(subscriptionService.getMySubscriptions(userId));
    }


    /**
     * 구독 취소 (다음 결제 중단, 만료일까지 이용 가능)
     * DELETE /api/me/subscriptions
     */
    @DeleteMapping
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal Long userId
    ) {
        subscriptionService.cancel(userId);
        return ResponseEntity.noContent().build();
    }
}