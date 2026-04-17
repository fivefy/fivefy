package com.fivefy.domain.subscription.service;

import com.fivefy.domain.pointorder.entity.PointOrder;
import com.fivefy.domain.pointorder.repository.PointOrderRepository;
import com.fivefy.domain.subscription.dto.SubscriptionPurchaseRequest;
import com.fivefy.domain.subscription.dto.SubscriptionRefundRequest;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PointOrderRepository pointOrderRepository;
    private final WalletRepository walletRepository;
    private final PointHistoryRepository pointHistoryRepository;

    /**
     * 구독 구매
     *
     * 흐름:
     * 1. 이미 활성 구독이 있는지 확인
     * 2. 플랜 가격 확인
     * 3. 지갑에서 포인트 차감 (FREE면 스킵)
     * 4. 구독 생성
     * 5. PointHistory 기록
     */
    @Transactional
    public SubscriptionResponse purchase(Long userId, SubscriptionPurchaseRequest request) {
        SubscriptionPlanType planType = request.planType();
        Long price = planType.getPrice();
        LocalDateTime now = LocalDateTime.now();
        String orderNumber = "SUB-" + UUID.randomUUID().toString().substring(0, 8);

        // 1. FREE 1회 제한
        if (planType == SubscriptionPlanType.FREE) {
            List<PointOrder> userOrders = pointOrderRepository.findAllByUserId(userId);
            boolean alreadyUsedFree = userOrders.stream()
                    .anyMatch(o -> o.getPlanType() == SubscriptionPlanType.FREE);
            if (alreadyUsedFree) {
                throw new IllegalStateException("무료 체험은 1회만 가능합니다.");
            }
        }

        // 2. PointOrder 생성
        PointOrder pointOrder = PointOrder.create(userId, planType, orderNumber);
        pointOrder.success();
        pointOrderRepository.save(pointOrder);

        // 3. 내 지갑에서 포인트 차감 (FREE가 아닐 때)
        if (price > 0) {
            Wallet wallet = walletRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("지갑 없음"));

            if (wallet.getTotalBalance() < price) {
                throw new IllegalStateException(
                        "포인트 부족. 필요: " + price + ", 보유: " + wallet.getTotalBalance());
            }

            wallet.useBalance(price);

            // 4. PointHistory 기록
            pointHistoryRepository.save(
                    PointHistory.create(
                            wallet.getId(),
                            PointType.PAID,
                            PointHistoryType.USE,
                            price,
                            wallet.getBalance(),
                            "구독 결제 (" + planType.getDescription() + ")"
                    )
            );
        }

        // 5. 구독 생성
        LocalDateTime expiryDate = Subscription.calculateExpiryDate(planType, now);
        LocalDateTime nextBillingDate = Subscription.calculateNextBillingDate(planType, now);

        Subscription subscription = Subscription.create(
                userId,
                pointOrder.getId(),
                planType,
                now,
                expiryDate,
                nextBillingDate
        );
        subscriptionRepository.save(subscription);

        return SubscriptionResponse.from(subscription);
    }

    /**
     * 내 구독 조회
     * userId로 PointOrder 조회
     * pointOrderId로 Subscription 조회
     */
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getMySubscriptions(Long userId) {

        List<Long> pointOrderIds = pointOrderRepository.findAllByUserId(userId).stream()
                .map(PointOrder::getId)
                .toList();

        return subscriptionRepository.findAllByPointOrderIdIn(pointOrderIds).stream()
                .map(SubscriptionResponse::from)
                .toList();
    }

    /**
     * 구독 환불 - 포인트 반환
     *
     * 흐름:
     * 1. 구독 조회 + 소유자 검증
     * 2. 구독 환불 처리
     * 3. 지갑 포인트 반환
     * 4. PointHistory 기록
     */
    @Transactional
    public SubscriptionResponse refund(Long userId, SubscriptionRefundRequest request) {
        // 1. 구독 조회
        Subscription subscription = subscriptionRepository.findById(request.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException("구독 내역을 찾을 수 없습니다."));

        if (!subscription.getUserId().equals(userId)) {
            throw new IllegalStateException("본인의 구독만 환불할 수 있습니다.");
        }

        // 상태 검증 (엔티티에서 이미 하지만 한 번 더 명확하게)
        if (subscription.getStatus() != SubscriptionStatus.INACTIVE) {
            throw new IllegalStateException("환불 가능한 상태가 아님");
        }

        // 구독 환불 처리 : 구독 타입 변경
        subscription.refund();

        // PointOrder 조회
        PointOrder order = pointOrderRepository.findById(subscription.getPointOrderId())
                .orElseThrow(() -> new IllegalArgumentException("주문 없음"));

        // CashOrderStatus.REFUNDED
        order.refund();

        // price는 구독의 플랜 타입(enums)의 가격
        Long price = subscription.getPlanType().getPrice();

        // 5. 포인트 반환 (FREE가 아닐 때)
        if (price > 0) {
            Wallet wallet = walletRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("지갑을 찾을 수 없습니다."));
            wallet.chargeBalance(price);

            // 5. PointHistory 기록
            pointHistoryRepository.save(
                PointHistory.create(
                        wallet.getId(),
                        PointType.PAID,
                        PointHistoryType.REFUND,
                        price,
                        wallet.getBalance(),
                        "구독 환불 (" + subscription.getPlanType().name() + ")"
                )
            );
        }

        return SubscriptionResponse.from(subscription);
    }

    /**
     * 구독 취소 - 다음 결제 중단, 만료일까지 이용 가능, 포인트 반환 없음
     */
    @Transactional
    public void cancel(Long userId) {

        Subscription subscription = subscriptionRepository
                .findByUserIdAndPlanType(userId, SubscriptionPlanType.RECURRING)
                .orElseThrow(() -> new IllegalArgumentException("정기 구독 없음"));

        subscription.cancel();
    }
}