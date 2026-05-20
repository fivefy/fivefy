package com.fivefy.domain.cashorder.scheduler;

import com.fivefy.domain.billingkey.entity.BillingKey;
import com.fivefy.domain.billingkey.repository.BillingKeyRepository;
import com.fivefy.domain.cashorder.service.CashOrderService;
import com.fivefy.domain.pointorder.entity.PointOrder;
import com.fivefy.domain.pointorder.repository.PointOrderRepository;
import com.fivefy.domain.subscription.entity.Subscription;
import com.fivefy.domain.subscription.enums.SubscriptionPlanType;
import com.fivefy.domain.subscription.enums.SubscriptionStatus;
import com.fivefy.domain.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CashOrderScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final BillingKeyRepository billingKeyRepository;
    private final CashOrderService cashOrderService;
    private final PointOrderRepository pointOrderRepository;

    /**
     * 정기 포인트 자동 충전 (빌링키 카드 청구)
     * 매일 08:01 — 구독 결제 스케줄러(09:01)보다 1시간 먼저 실행
     *
     * 대상:
     * - 활성 빌링키
     * - nextChargeDate <= now
     * - ACTIVE 상태의 RECURRING_AUTO 구독 보유 사용자
     */
    @Scheduled(cron = "${scheduler.charge-cron}")
    public void processRecurringCharges() {
        LocalDateTime now = LocalDateTime.now();
        log.info("[정기충전 스케줄러] 실행 시작 — {}", now);

        List<BillingKey> billingKeys =
                billingKeyRepository.findAllByActiveTrueAndNextChargeDateLessThanEqual(now);

        log.info("[정기충전 스케줄러] 대상 빌링키 수: {}", billingKeys.size());

        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;

        for (BillingKey billingKey : billingKeys) {
            Long userId = billingKey.getUserId();

            try {
                List<PointOrder> pointOrders = pointOrderRepository.findAllByUserId(userId);

                if (pointOrders.isEmpty()) {
                    log.warn("[정기충전 스케줄러] PointOrder 없음 — userId={}", userId);
                    skipCount++;
                    continue;
                }

                List<Long> pointOrderIds = pointOrders.stream()
                        .map(PointOrder::getId)
                        .toList();

                Subscription subscription = subscriptionRepository
                        .findByPointOrderIdInAndPlanTypeAndStatus(
                                pointOrderIds,
                                SubscriptionPlanType.RECURRING_AUTO,
                                SubscriptionStatus.ACTIVE
                        )
                        .orElse(null);

                if (subscription == null) {
                    log.warn("[정기충전 스케줄러] 활성 정기 구독 없음 — userId={}", userId);
                    skipCount++;
                    continue;
                }

                cashOrderService.processRecurringCharge(billingKey);
                successCount++;

            } catch (Exception e) {
                log.error("[정기충전 스케줄러] 실패 — userId={}, billingKeyId={}, 사유={}",
                        userId, billingKey.getId(), e.getMessage());
                failCount++;
            }
        }

        log.info("[정기충전 스케줄러] 완료 — 성공: {}, 실패: {}, 스킵: {}",
                successCount, failCount, skipCount);
    }
}