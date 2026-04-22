package com.fivefy.domain.subscription.controller;


import com.fivefy.domain.pointorder.service.PointOrderService;
import com.fivefy.domain.subscription.dto.SubscriptionResponse;
import com.fivefy.domain.subscription.entity.Subscription;
import com.fivefy.domain.subscription.enums.SubscriptionPlanType;
import com.fivefy.domain.subscription.enums.SubscriptionStatus;
import com.fivefy.domain.subscription.repository.SubscriptionRepository;
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
    private final PointOrderService pointOrderService;
    private final SubscriptionRepository subscriptionRepository;


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

    /**
     * [테스트 전용] 구독 포인트 차감 수동 실행
     * POST /api/v1/subscriptions/test-recurring
     *
     * 스케줄러(매일 09:00)를 기다리지 않고 즉시 실행.
     * 로그인한 유저의 RECURRING + ACTIVE 구독을 찾아 포인트 차감 → 갱신.
     * 테스트 완료 후 이 메서드는 삭제할 것.
     */
    @PostMapping("/test-recurring")
    public ResponseEntity<String> testRecurring(
            @AuthenticationPrincipal Long userId
    ) {
        Subscription subscription = subscriptionRepository
                .findByUserIdAndPlanTypeAndStatus(
                        userId,
                        SubscriptionPlanType.RECURRING,
                        SubscriptionStatus.ACTIVE
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "RECURRING + ACTIVE 구독이 없습니다. 먼저 정기구독을 구매하세요."
                ));

        pointOrderService.processRecurringPayment(subscription);

        return ResponseEntity.ok("구독 포인트 차감 완료 — 구독 상태와 지갑을 조회해서 확인하세요.");
    }
}