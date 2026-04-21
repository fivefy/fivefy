package com.fivefy.domain.pointorder.service;

import com.fivefy.common.lock.annotation.RedissonLock;
import com.fivefy.domain.pointorder.dto.PointOrderPurchaseRequest;
import com.fivefy.domain.pointorder.dto.PointOrderRefundRequest;
import com.fivefy.domain.pointorder.entity.PointOrder;
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
import com.fivefy.domain.wallet.repository.PointHistoryRepository;
import com.fivefy.domain.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    /**
     * 구독 구매 (포인트 → 구독)
     *
     * 흐름:
     * 1. FREE 플랜 중복 체크 (계정당 1회)
     * 2. PointOrder 생성 → success() (PENDING → SUCCESS 즉시)
     * 3. 포인트 차감: 무료 → 유료 순서 (useBalanceWithPriority)
     * 4. PointHistory(USE) 기록
     * 5. Subscription 생성 (FREE→ACTIVE / 유료→INACTIVE)
     */
    // CashOrder와 PointOrder 모두 wallet와  같은 Wallet ID 수정
    @RedissonLock(key = "'wallet:' + #userId")
    @Transactional
    public SubscriptionResponse purchase(Long userId, PointOrderPurchaseRequest request) {
        SubscriptionPlanType planType = request.planType();
        Long price = planType.getPrice();
        LocalDateTime now = LocalDateTime.now();
        String orderNumber = "SUB-" + UUID.randomUUID().toString().substring(0, 8);

        // 1. FREE 1회 제한(중복 체크)
        if (planType == SubscriptionPlanType.FREE) {
            List<PointOrder> userOrders = pointOrderRepository.findAllByUserId(userId);
            boolean alreadyUsedFree = userOrders.stream()
                    .anyMatch(o -> o.getPlanType() == SubscriptionPlanType.FREE);
            if (alreadyUsedFree) {
                throw new IllegalStateException("무료 체험은 1회만 가능합니다.");
            }
        }

        // 2. PointOrder 생성 → 즉시 SUCCESS
        PointOrder pointOrder = PointOrder.create(userId, planType, orderNumber);
        pointOrder.success();
        pointOrderRepository.save(pointOrder);

        // 3. 내 지갑에서 포인트 차감 (무료 포인트 먼저 소진 후 유료 차감)
        if (price > 0) {
            Wallet wallet = walletRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("지갑 없음"));

            wallet.useBalanceWithPriority(price);  // 무료 → 유료 순서 차감

            // 4. PointHistory 기록 (무료/유료 구분)
            // 차감 내역: eventBalance 변동이 있으면 FREE, 나머지 PAID
            // 단순화: 전액 PAID로 기록 (상세 구분은 추후 확장)
            pointHistoryRepository.save(PointHistory.create(
                wallet.getId(),
                PointType.PAID,
                PointHistoryType.USE,
                price,
                wallet.getBalance(),  // 차감 후 유료 잔액 스냅샷
                "구독 결제 (" + planType.getDescription() + ")"
            ));
        }

        // 5. 구독 생성
        LocalDateTime expiryDate = Subscription.calculateExpiryDate(planType, now);
        LocalDateTime nextBillingDate = Subscription.calculateNextBillingDate(planType, now);

        Subscription subscription = Subscription.create(
                userId, pointOrder.getId(), planType, now, expiryDate, nextBillingDate
        );
        subscriptionRepository.save(subscription);

        return SubscriptionResponse.from(subscription);
    }


    /**
     * 구독 환불 (포인트 반환)
     * INACTIVE(결제 완료, 아직 미사용) 상태만 환불 가능
     *
     * 흐름:
     * 1. 구독(subscription) 조회 + 소유자 검증 + INACTIVE(비활성화) 확인
     * 2. 구독 환불 처리        : subscription.refund() → CANCELED
     * 3.                     : pointOrder.refund() → REFUNDED
     * 4. 지갑 포인트 반환      : wallet.chargeBalance(price) → 포인트 반환
     * 5. 포인트 사용 내역 기록 : PointHistory(REFUND) 기록
     * @param userId
     * @param request
     * @return
     */
    // CashOrder와 PointOrder 모두 wallet와  같은 Wallet ID 수정
    @RedissonLock(key = "'wallet:' + #userId")
    @Transactional
    public SubscriptionResponse refund(Long userId, PointOrderRefundRequest request) {
        // 1. 구독 조회 및 검증
        Subscription subscription = subscriptionRepository.findById(request.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException("구독 내역을 찾을 수 없습니다."));

        if (!subscription.getUserId().equals(userId)) {
            throw new IllegalStateException("본인의 구독만 환불할 수 있습니다.");
        }
        if (subscription.getStatus() != SubscriptionStatus.INACTIVE) {
            throw new IllegalStateException("환불 가능한 상태가 아닙니다. (INACTIVE만 가능)");
        }

        // 2. 구독 상태 변경 (INACTIVE → CANCELED), 엔티티에서 하지만, 한 번 더
        subscription.refund();

        // 3. PointOrder 상태 변경 (SUCCESS → REFUNDED)
        PointOrder order = pointOrderRepository.findById(subscription.getPointOrderId())
                .orElseThrow(() -> new IllegalArgumentException("주문 없음"));
        // CashOrderStatus.REFUNDED
        order.refund();


        // price는 구독의 플랜 타입(enums)의 가격
        Long price = subscription.getPlanType().getPrice();

        // 4. 포인트 반환 (FREE가 아닐 때)
        if (price > 0) {
            Wallet wallet = walletRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("지갑 없음"));

            wallet.chargeBalance(price);  // 유료 포인트로 반환(무료로 구매해서 반환하더라도 일단 이걸로 진행)

            // 5. PointHistory 기록 (REFUND)
            pointHistoryRepository.save(PointHistory.create(
                wallet.getId(),
                PointType.PAID,
                PointHistoryType.REFUND,
                price,
                wallet.getBalance(),  // 반환 후 유료 잔액 스냅샷
                "구독 환불 (" + subscription.getPlanType().name() + ")"
            ));
        }

        return SubscriptionResponse.from(subscription);
    }

    /**
     * 정기 구독(RECURRING) 자동 결제 처리
     * RecurringPaymentScheduler에서 호출
     * 잔액 부족 시 구독 만료 처리
     *      미구현 문제1 : 3개월 이상 구독인데, 2개월차에 부족하면?
     *              해결 방안 1 : 3개월치 미리 구독비를 낸다. 이러면 정기 구독 취소도 문제 없음
     */
    @RedissonLock(key = "'cashOrder:' + #userId")
    public void processRecurringPayment(Subscription subscription) {
        Long userId = subscription.getUserId();
        Long price = SubscriptionPlanType.RECURRING.getPrice();

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("지갑 없음"));

        // 잔액 부족 시 구독 만료
        if (wallet.getTotalBalance() < price) {
            log.warn("정기 구독 잔액 부족 — userId={}, 필요={}, 보유={}",
                   userId, price, wallet.getTotalBalance());
            subscription.expire();
            subscriptionRepository.save(subscription);
            return;
        }

        // PointOrder 생성 (정기 결제 이력)
        String orderNumber = "REC-" + UUID.randomUUID().toString().substring(0, 8);
        PointOrder pointOrder = PointOrder.create(userId, SubscriptionPlanType.RECURRING, orderNumber);
        pointOrder.success();
        pointOrderRepository.save(pointOrder);

        // 포인트 차감 (무료 → 유료 순서)
        wallet.useBalanceWithPriority(price);

        // PointHistory 기록
        pointHistoryRepository.save(PointHistory.create(
                wallet.getId(),
                PointType.PAID,
                PointHistoryType.USE,
                price,
                wallet.getBalance(),
                "정기 구독 자동 결제 (RECURRING)"
        ));

        // 구독 갱신 (nextBillingDate + 1개월, expiryDate + 1개월)
        subscription.renew();
        subscriptionRepository.save(subscription);

        log.info("정기 구독 결제 완료 — userId={}, orderNumber={}", userId, orderNumber);
    }
}
