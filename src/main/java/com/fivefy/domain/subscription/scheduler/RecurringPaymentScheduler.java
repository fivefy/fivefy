package com.fivefy.domain.subscription.scheduler;

import com.fivefy.domain.billingkey.entity.BillingKey;
import com.fivefy.domain.billingkey.repository.BillingKeyRepository;
import com.fivefy.domain.cashorder.service.CashOrderService;
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
public class RecurringPaymentScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final PointOrderService pointOrderService;
    private final BillingKeyRepository billingKeyRepository;
    private final CashOrderService cashOrderService;

    /**
     * ① 정기 포인트 자동 충전 (빌링키 카드 청구)
     * 매월 1일 오전 8시 — 구독 결제 스케줄러(9시)보다 1시간 먼저 실행
     * → 포인트를 먼저 채워두고, 구독 차감이 성공하도록
     *
     * 대상: active=true 인 BillingKey 전체
     */
    @Scheduled(cron = "0 0 8 1 * *")
    public void processRecurringCharges() {
        LocalDateTime now = LocalDateTime.now();
        log.info("[정기충전 스케줄러] 실행 시작 — {}", now);

        List<BillingKey> targets = billingKeyRepository.findAllByActiveTrue();
        log.info("[정기충전 스케줄러] 대상 빌링키 수: {}", targets.size());

        int successCount = 0;
        int failCount = 0;

        for (BillingKey billingKey : targets) {
            try {
                cashOrderService.processRecurringCharge(billingKey);
                successCount++;
            } catch (Exception e) {
                log.error("[정기충전 스케줄러] 실패 — userId={}, 사유={}",
                        billingKey.getUserId(), e.getMessage());
                failCount++;
            }
        }

        log.info("[정기충전 스케줄러] 완료 — 성공: {}, 실패: {}", successCount, failCount);
    }

    /**
     * ② 정기 구독 포인트 차감
     * 매일 오전 9시 실행
     * → 충전된 포인트에서 45P 차감 + 구독 갱신
     *
     * 대상: RECURRING + ACTIVE + nextBillingDate 지난 구독
     */
    @Scheduled(cron = "0 0 9 * * *")
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
     * ③ 구독 만료 처리
     * 매일 자정 실행
     * → expiryDate 지난 ACTIVE/INACTIVE 구독 → EXPIRE
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void expireSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        log.info("[만료 스케줄러] 실행 시작 — {}", now);

        List<Subscription> expiredTargets = subscriptionRepository
                .findAllByStatusInAndExpiryDateBefore(
                        List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.INACTIVE),
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