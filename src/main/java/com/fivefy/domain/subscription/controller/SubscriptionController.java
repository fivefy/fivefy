package com.fivefy.domain.subscription.controller;


import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.pointorder.service.PointOrderService;
import com.fivefy.domain.subscription.dto.SubscriptionResponse;
import com.fivefy.domain.subscription.entity.Subscription;
import com.fivefy.domain.subscription.enums.SubscriptionErrorCode;
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
     * [테스트 전용] 구독 포인트 결제 수동 실행
     * POST /api/v1/subscriptions/test-recurring
     *
     * 스케줄러(매일 09:00)를 기다리지 않고 즉시 실행.
     * 로그인한 유저의 RECURRING + ACTIVE 구독을 찾아 포인트 결제 → 갱신.
     * 테스트 완료 후 이 메서드는 삭제할 것.
     */
    @PostMapping("/test-recurring")
    public ResponseEntity<String> testRecurring(
            @AuthenticationPrincipal Long userId
    ) {
        Subscription subscription = subscriptionRepository
                .findByUserIdAndPlanTypeAndStatus(
                        userId,
                        SubscriptionPlanType.RECURRING_AUTO,
                        SubscriptionStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(
                        SubscriptionErrorCode.ERR_SUBSCRIPTION_RECURRING_NOT_FOUND
                ));

        pointOrderService.processRecurringPayment(subscription);

        return ResponseEntity.ok("구독 포인트 차감 완료 — 구독 상태와 지갑을 조회해서 확인하세요.");
    }

    /**
     * [테스트 전용] 1개월 스킵 : 정기 구독 자동 활성화. 1개월 지나면 알아서 추가되야 함
     * POST /api/me/subscriptions/test-skip-month
     * 내 RECURRING 구독의 nextBillingDate, expiryDate를 -1개월
     * → 스케줄러 조건(nextBillingDate < now) 즉시 충족
     */
    @PostMapping("/test-skip-month")
    public ResponseEntity<SubscriptionResponse> testSkipMonth(
            @AuthenticationPrincipal Long userId
    ) {
        Subscription sub = subscriptionRepository
                .findByUserIdAndPlanTypeAndStatus(
                        userId,
                        SubscriptionPlanType.RECURRING,
                        SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        SubscriptionErrorCode.ERR_SUBSCRIPTION_RECURRING_NOT_FOUND
                ));

        sub.skipOneMonth(); // 엔티티 메서드 추가 필요
        subscriptionRepository.save(sub);
        return ResponseEntity.ok(SubscriptionResponse.from(sub));
    }
}