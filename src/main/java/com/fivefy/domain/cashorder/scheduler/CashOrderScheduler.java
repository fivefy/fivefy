package com.fivefy.domain.cashorder.scheduler;

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
     * 매월 1일 08:00 — 구독 결제 스케줄러(09:00)보다 1시간 먼저 실행
     * 대상: RECURRING + ACTIVE + nextBillingDate 지난 구독 보유 유저
     */
    @Scheduled(cron = "${scheduler.charge-cron}")
    public void processRecurringCharges() {
        LocalDateTime now = LocalDateTime.now();
        log.info("[정기충전 스케줄러] 실행 시작 — {}", now);

        // 카드 자동 청구 대상은 RECURRING_AUTO만
        List<Subscription> targets = subscriptionRepository
                .findAllByPlanTypeAndStatusAndNextBillingDateBefore(
                        SubscriptionPlanType.RECURRING_AUTO,
                        SubscriptionStatus.ACTIVE,
                        now
                );

        log.info("[정기충전 스케줄러] 대상 구독 수: {}", targets.size());


        // PointOrder 일괄 조회로 N+1 방지
        List<Long> pointOrderIds = targets.stream()
               .map(Subscription::getPointOrderId)
               .distinct()
               .toList();
        Map<Long, Long> pointOrderToUserId = pointOrderRepository.findAllById(pointOrderIds)
               .stream()
               .collect(Collectors.toMap(PointOrder::getId, PointOrder::getUserId));

        int successCount = 0;
        int failCount = 0;

        for (Subscription subscription : targets) {
            // PointOrder를 일괄 조회로 개선 필요
            Long userId = pointOrderToUserId.get(subscription.getPointOrderId());

            if (userId == null) {
                log.error("[정기충전 스케줄러] PointOrder 조회 실패 — subscriptionId={}",
                        subscription.getId());
                failCount++;
                continue;
            }

            try {
                billingKeyRepository.findByUserIdAndActiveTrue(userId)
                        .ifPresentOrElse(
                                billingKey -> cashOrderService.processRecurringCharge(billingKey),
                                () -> log.warn("[정기충전 스케줄러] 빌링키 없음 — userId={}",
                                        userId)
                        );
                successCount++;
            } catch (Exception e) {
                log.error("[정기충전 스케줄러] 실패 — userId={}, subscriptionId={}, 사유={}",
                                    userId, subscription.getId(), e.getMessage());
                failCount++;
            }
        }

        log.info("[정기충전 스케줄러] 완료 — 성공: {}, 실패: {}", successCount, failCount);
    }
}