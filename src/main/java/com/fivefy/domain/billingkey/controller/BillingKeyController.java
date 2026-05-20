package com.fivefy.domain.billingkey.controller;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.billingkey.enums.BillingKeyErrorCode;
import com.fivefy.domain.billingkey.dto.BillingKeyRegisterRequest;
import com.fivefy.domain.billingkey.dto.BillingKeyResponse;
import com.fivefy.domain.billingkey.entity.BillingKey;
import com.fivefy.domain.billingkey.repository.BillingKeyRepository;
import com.fivefy.domain.billingkey.service.BillingKeyService;
import com.fivefy.domain.cashorder.service.CashOrderService;
import com.fivefy.domain.pointorder.entity.PointOrder;
import com.fivefy.domain.pointorder.repository.PointOrderRepository;
import com.fivefy.domain.subscription.entity.Subscription;
import com.fivefy.domain.subscription.enums.SubscriptionErrorCode;
import com.fivefy.domain.subscription.enums.SubscriptionPlanType;
import com.fivefy.domain.subscription.enums.SubscriptionStatus;
import com.fivefy.domain.subscription.repository.SubscriptionRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing-keys")
@RequiredArgsConstructor
public class BillingKeyController {

    private final BillingKeyService billingKeyService;
    private final BillingKeyRepository billingKeyRepository;
    private final CashOrderService cashOrderService;
    private final PointOrderRepository pointOrderRepository;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * 카드(빌링키) 등록
     * POST /api/v1/billing-keys
     *
     * 프론트 흐름:
     * 1. 포트원 SDK로 카드 등록 → billingKeyId 수령 : 아임포트? 인걸로 할 수 있다 함 : 이거 없어졌는데
     *                                              -> 이게 포트원이래.
     * 2. 이 API에 billingKeyId 전달
     * 3. 서버가 포트원에서 빌링키 조회 후 DB 저장
     */
    @PostMapping
    public ResponseEntity<BillingKeyResponse> register(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody BillingKeyRegisterRequest request
    ) {
        return ResponseEntity.ok(billingKeyService.register(userId, request));
    }

    /**
     * 카드(빌링키) 해지
     * DELETE /api/v1/billing-keys/{billingKeyId}
     */
    @DeleteMapping("/{billingKeyId}")
    public ResponseEntity<Void> deactivate(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long billingKeyId
    ) {
        billingKeyService.deactivate(userId, billingKeyId);
        return ResponseEntity.noContent().build();
    }

    /**
     * [테스트 전용] 빌링키 카드 청구 수동 실행
     * POST /api/v1/billing-keys/test-charge
     *
     * 스케줄러(매일 08:01)를 기다리지 않고 즉시 실행.
     * 로그인한 유저의 활성 빌링키로 포트원에 카드 청구 → 포인트 충전.
     * 테스트 완료 후 이 메서드는 삭제할 것.
     */
    @PostMapping("/test-charge")
    public ResponseEntity<String> testCharge(
            @AuthenticationPrincipal Long userId
    ) {
        // 빌링키  체크
        BillingKey billingKey = billingKeyRepository.findByUserIdAndActiveTrue(userId)
                .orElseThrow(() -> new BusinessException(
                        BillingKeyErrorCode.ERR_BILLING_KEY_ACTIVE_NOT_FOUND)
                );

        List<Long> pointOrderIds = pointOrderRepository.findAllByUserId(userId)
                .stream()
                .map(PointOrder::getId)
                .toList();

        Subscription subscription = subscriptionRepository
                .findByPointOrderIdInAndPlanTypeAndStatus(
                        pointOrderIds,
                        SubscriptionPlanType.RECURRING_AUTO,
                        SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        SubscriptionErrorCode.ERR_SUBSCRIPTION_NOT_FOUND));

        cashOrderService.processRecurringCharge(billingKey, subscription);

        return ResponseEntity.ok("카드 청구 완료 — 지갑을 조회해서 포인트 충전을 확인하세요.");
    }
}