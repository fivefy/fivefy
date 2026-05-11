package com.fivefy.domain.pointorder.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.common.lock.annotation.RedissonLock;
import com.fivefy.domain.notification.entity.NotificationOutbox;
import com.fivefy.domain.notification.enums.NotificationType;
import com.fivefy.domain.notification.repository.NotificationOutboxRepository;
import com.fivefy.domain.pointorder.dto.PointOrderPurchaseRequest;
import com.fivefy.domain.pointorder.entity.PointOrder;
import com.fivefy.domain.pointorder.enums.PointOrderErrorCode;
import com.fivefy.domain.pointorder.repository.PointOrderRepository;
import com.fivefy.domain.subscription.dto.SubscriptionResponse;
import com.fivefy.domain.subscription.entity.Subscription;
import com.fivefy.domain.subscription.enums.SubscriptionPlanType;
import com.fivefy.domain.subscription.enums.SubscriptionStatus;
import com.fivefy.domain.subscription.repository.SubscriptionRepository;
import com.fivefy.domain.wallet.entity.PointHistory;
import com.fivefy.domain.wallet.entity.Wallet;
import com.fivefy.domain.wallet.enums.PointHistoryType;
import com.fivefy.domain.wallet.enums.PointType;
import com.fivefy.domain.wallet.enums.WalletErrorCode;
import com.fivefy.domain.wallet.repository.PointHistoryRepository;
import com.fivefy.domain.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointOrderService {

    private final SubscriptionRepository subscriptionRepository;
    private final PointOrderRepository pointOrderRepository;
    private final WalletRepository walletRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final NotificationOutboxRepository outboxRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

    /**
     * 구독 구매 (포인트 → 구독)
     *
     * 흐름:
     * 1. FREE 플랜 중복 체크 (계정당 1회)
     * 2. RECURRING(정기 구독) 중복 체크 (활성/비활성 구독이 이미 있으면 불가)
     * 3. PointOrder 생성 → success()
     * 4. 구매한 만큼 지갑에서 포인트 차감
     * 5. PointHistory(USE) 기록
     * 6. Subscription 생성
     *      - 구독 상품 1개 : 구매 즉시 활성화
     */
    // CashOrder와 PointOrder 모두 wallet와  같은 Wallet ID 수정
    @RedissonLock(key = "'wallet:' + #userId")
    @Transactional
    public SubscriptionResponse purchase(Long userId, PointOrderPurchaseRequest request) {
        SubscriptionPlanType planType = request.planType();
        LocalDateTime now = LocalDateTime.now();
        String orderNumber = "SUB-" + UUID.randomUUID().toString().substring(0, 8);

        log.info("[PointOrder] 구독 구매 요청 userId={}, planType={}", userId, planType);

        // 1. FREE 1회 제한(중복 체크)
        if (planType == SubscriptionPlanType.FREE) {
            List<PointOrder> userOrders = pointOrderRepository.findAllByUserId(userId);
            boolean alreadyUsedFree = userOrders.stream()
                    .anyMatch(o -> o.getPlanType() == SubscriptionPlanType.FREE);
            if (alreadyUsedFree) {
                throw new BusinessException(PointOrderErrorCode.ERR_FREE_PLAN_ALREADY_USED);
            }
        }

        // 2. RECURRING/RECURRING_AUTO 중복 가입 방지 (ACTIVE만 체크)
        if (planType == SubscriptionPlanType.RECURRING || planType == SubscriptionPlanType.RECURRING_AUTO) {
            // userId → pointOrderIds 변환 후 구독 목록 조회
            List<Long> pointOrderIds = pointOrderRepository.findAllByUserId(userId)
                    .stream()
                    .map(PointOrder::getId)
                    .toList();

            boolean alreadyActive = subscriptionRepository
                    .findAllByPointOrderIdIn(pointOrderIds).stream()
                    .anyMatch(s -> (s.getPlanType() == SubscriptionPlanType.RECURRING
                                        || s.getPlanType() == SubscriptionPlanType.RECURRING_AUTO)
                            && s.getStatus() == SubscriptionStatus.ACTIVE);

            if (alreadyActive) {
                throw new BusinessException(PointOrderErrorCode.ERR_SUBSCRIPTION_ALREADY_ACTIVE);
            }
        }

        // 3. PointOrder 생성 → 즉시 SUCCESS
        PointOrder pointOrder = PointOrder.create(userId, planType, orderNumber);
        pointOrder.success();
        pointOrderRepository.save(pointOrder);

        // 4. 내 지갑에서 포인트 차감 (무료 포인트 먼저 소진 후 유료 차감)
        Long price = planType.getPrice();
        if (price > 0) {
            Wallet wallet = walletRepository.findByUserId(userId)
                    .orElseThrow(() -> new BusinessException(WalletErrorCode.ERR_WALLET_NOT_FOUND));
            // (무료 포인트 먼저 소진 후 유료 차감)
            wallet.useBalanceWithPriority(price);

            // 5. PointHistory 기록
            pointHistoryRepository.save(PointHistory.create(
                    wallet.getId(),
                    PointType.PAID,
                    PointHistoryType.USE,
                    price,
                    wallet.getBalance(),
                    "구독 결제 (" + planType.getDescription() + ")",
                    null,      // cashOrderId
                    pointOrder.getId()
            ));
        }


        // 6. 구독 생성 → 즉시 ACTIVE
        Subscription subscription = Subscription.create(
                pointOrder.getId(), planType, now
        );
        subscriptionRepository.save(subscription);

        log.info("구독 구매 완료 — userId={}, planType={}, price={}P, expiryDate={}",
                userId, planType, price, subscription.getExpiryDate());

        String content = buildSubscribeContent(planType, subscription.getExpiryDate());
        outboxRepository.save(NotificationOutbox.create(
                NotificationType.SUBSCRIBE, userId, null,
                subscription.getId(), content
        ));

        return SubscriptionResponse.from(subscription);
    }

    /**
     * 정기 구독(RECURRING) 자동 결제 처리
     * RecurringPaymentScheduler에서 호출 (매월 09:00)
     * 매월 50P 차감 → nextBillingDate, expiryDate +1개월
     * 잔액 부족 시 구독 만료
     *
     * @param subscription 갱신 대상 구독
     * @param userId       PointOrder.getUserId()로 추출한 유저 ID
     */
    @RedissonLock(key = "'wallet:' + #userId")
    @Transactional
    public void processRecurringPayment(Subscription subscription, Long userId) {  // userId 파라미터 추가
        Long price = SubscriptionPlanType.RECURRING.getPrice(); // 50P

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(WalletErrorCode.ERR_WALLET_NOT_FOUND));

        // 잔액 부족 시 구독 만료
        if (wallet.getTotalBalance() < price) {
            log.warn("[정기구독] 잔액 부족 — userId={}, 필요={}P, 보유={}P",
                    userId, price, wallet.getTotalBalance());
            subscription.expire();
            subscriptionRepository.save(subscription);

            outboxRepository.save(NotificationOutbox.create(
                    NotificationType.SUBSCRIPTION_EXPIRE, userId, null,
                    subscription.getId(), "포인트 잔액이 부족하여 구독이 만료되었습니다."
            ));

            return;
        }

        // PointOrder 생성 (정기 결제 이력)
        String orderNumber = "REC-" + UUID.randomUUID().toString().substring(0, 8);
        PointOrder pointOrder = PointOrder.create(userId, SubscriptionPlanType.RECURRING, orderNumber);
        pointOrder.success();
        pointOrderRepository.save(pointOrder);

        // 포인트 차감
        wallet.useBalanceWithPriority(price);

        // PointHistory 기록
        pointHistoryRepository.save(PointHistory.create(
                wallet.getId(),
                PointType.PAID,
                PointHistoryType.USE,
                price,
                wallet.getBalance(),
                "정기 구독 자동 결제",
                null,      // cashOrderId
                pointOrder.getId()
        ));

        // 구독 갱신 (nextBillingDate +1개월, expiryDate +1개월)
        subscription.renew();
        subscriptionRepository.save(subscription);

        log.info("[정기구독] 결제 완료 — userId={}, orderNumber={}, price={}P",
                userId, orderNumber, price);

        outboxRepository.save(NotificationOutbox.create(
                NotificationType.SUBSCRIBE, userId, null,
                pointOrder.getId(),
                "정기 구독이 갱신되었습니다. 다음 만료일: " + subscription.getExpiryDate().format(DATE_FORMATTER)
        ));
    }

    private String buildSubscribeContent(SubscriptionPlanType planType, LocalDateTime expiryDate) {
        return switch (planType) {
            case FREE -> "무료 체험 구독이 시작되었습니다. 만료일: " + expiryDate.format(DATE_FORMATTER);
            case RECURRING -> "단건 구독이 시작되었습니다. 만료일: " + expiryDate.format(DATE_FORMATTER);
            case RECURRING_AUTO -> "정기 구독이 시작되었습니다. 다음 갱신일: " + expiryDate.format(DATE_FORMATTER);
        };
    }
}
