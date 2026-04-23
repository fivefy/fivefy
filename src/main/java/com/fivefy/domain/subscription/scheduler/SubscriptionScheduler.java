package com.fivefy.domain.subscription.scheduler;

import com.fivefy.domain.pointorder.service.PointOrderService;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final PointOrderService pointOrderService;

    /**
     * 정기 구독 결제
     * 매월 1일 09:00
     * 대상: RECURRING + ACTIVE + nextBillingDate 지난 구독
     */
    @Scheduled(cron = "${scheduler.subscription-cron}")
    public void processRecurringPayments() {
        LocalDateTime now = LocalDateTime.now();
        log.info("[정기결제 스케줄러] 실행 시작 — {}", now);

        List<Subscription> targets = subscriptionRepository
                .findAllByPlanTypeAndStatusAndNextBillingDateBefore(
                        SubscriptionPlanType.RECURRING,
                        SubscriptionStatus.ACTIVE,
                        now
                );

        log.info("[정기결제 스케줄러] 대상 구독 수: {}", targets.size());

        int successCount = 0;
        int failCount = 0;

        for (Subscription subscription : targets) {
            try {
                pointOrderService.processRecurringPayment(subscription);
                successCount++;
            } catch (Exception e) {
                log.error("[정기결제 스케줄러] 결제 실패 — subscriptionId={}, userId={}, 사유={}",
                        subscription.getId(), subscription.getUserId(), e.getMessage());
                failCount++;
            }
        }

        log.info("[정기결제 스케줄러] 완료 — 성공: {}, 실패: {}", successCount, failCount);
    }

    /**
     * 구독 만료 처리
     * 매일 00:00
     * 대상: expiryDate 지난 ACTIVE 구독
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void expireSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        log.info("[만료 스케줄러] 실행 시작 — {}", now);

        List<Subscription> expiredTargets = subscriptionRepository
                .findAllByStatusAndExpiryDateBefore(
                        SubscriptionStatus.ACTIVE,
                        now
                );

        log.info("[만료 스케줄러] 만료 대상 수: {}", expiredTargets.size());

        for (Subscription subscription : expiredTargets) {
            try {
                subscription.expire();
                subscriptionRepository.save(subscription);
                log.info("[만료 스케줄러] 만료 처리 — subscriptionId={}, userId={}",
                        subscription.getId(), subscription.getUserId());
            } catch (Exception e) {
                log.error("[만료 스케줄러] 실패 — subscriptionId={}, 사유={}",
                        subscription.getId(), e.getMessage());
            }
        }

        log.info("[만료 스케줄러] 완료 — 처리 수: {}", expiredTargets.size());
    }
}