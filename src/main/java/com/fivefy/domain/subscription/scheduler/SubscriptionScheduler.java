package com.fivefy.domain.subscription.scheduler;

import com.fivefy.domain.notification.enums.NotificationType;
import com.fivefy.domain.notification.event.NotificationEvent;
import com.fivefy.domain.pointorder.service.PointOrderService;
import com.fivefy.domain.subscription.entity.Subscription;
import com.fivefy.domain.subscription.enums.SubscriptionPlanType;
import com.fivefy.domain.subscription.enums.SubscriptionStatus;
import com.fivefy.domain.subscription.repository.SubscriptionRepository;
import com.fivefy.domain.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final PointOrderService pointOrderService;
    private final ApplicationEventPublisher eventPublisher;
    private final SubscriptionService subscriptionService;

    /**
     * 정기 구독 결제
     * 매월 1일 09:00
     * 대상: RECURRING + ACTIVE + nextBillingDate 지난 구독
     */
    @Transactional
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
     *
     * 트랜젝션은 건별로 붙임(subscriptionService.expireOne())
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void expireSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        log.info("[만료 스케줄러] 실행 시작 — {}", now);

        // 기존 활성화 조회 → 활성화, 취소 조회
        /**
         * (기존) 활성화 조회
         * (수정) 활성화, 취소 조회
         * 이유 : 활성화 시 취소하는 경우 ACTIVE → 취소 → CANCELED → 만료일 경과 → EXPIRE
         * 이때, CANCELED를 조회하지 않으면 상태값이 만료되지 않고 CANCELED에 남기에 수정함
         */
        List<Subscription> expiredTargets = subscriptionRepository
                .findAllByStatusInAndExpiryDateBefore(
                        // SubscriptionStatus.ACTIVE,
                        List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED),
                        now
                );

        log.info("[만료 스케줄러] 만료 대상 수: {}", expiredTargets.size());

        for (Subscription subscription : expiredTargets) {
            try {
                subscriptionService.expireOne(subscription.getId()); // 건별 트랜잭션
                // subscriptionRepository.save(subscription);

                eventPublisher.publishEvent(NotificationEvent.of(
                        subscription.getUserId(),
                        NotificationType.SUBSCRIPTION_EXPIRE,
                        "구독이 만료되었습니다."
                ));

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