package com.fivefy.domain.subscription.service;

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

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
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
        // 1. 활성 구독 중복 체크
        if (subscriptionRepository.existsByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)) {
            throw new IllegalStateException("이미 활성 중인 구독이 있습니다.");
        }

        SubscriptionPlanType planType = request.planType();
        Long price = Subscription.getPlanPrice(planType);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = Subscription.calculateExpiryDate(planType, now);
        LocalDateTime nextBillingDate = Subscription.calculateNextBillingDate(planType, now);

        // 2. 지갑 조회
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("지갑을 찾을 수 없습니다."));

        // 3. 포인트 차감 (FREE가 아닐 때)
        if (price > 0) {
            if (wallet.getTotalBalance() < price) {
                throw new IllegalStateException(
                        "포인트가 부족합니다. 필요: " + price + ", 보유: " + wallet.getTotalBalance());
            }
            wallet.useBalance(price);

            // 5. PointHistory 기록
            PointHistory history = PointHistory.create(
                    wallet.getId(),
                    PointType.PAID,
                    PointHistoryType.USE,
                    price,
                    wallet.getBalance(),
                    "구독 결제 (" + planType.name() + ")"
            );
            pointHistoryRepository.save(history);
        }

        // 4. 구독 생성
        Subscription subscription = Subscription.create(
                userId, planType, now, expiryDate, nextBillingDate);
        subscriptionRepository.save(subscription);

        return SubscriptionResponse.from(subscription);
    }

    /**
     * 내 구독 조회
     */
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getMySubscriptions(Long userId) {
        return subscriptionRepository.findAllByUserId(userId).stream()
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

        Long price = Subscription.getPlanPrice(subscription.getPlanType());

        // 2. 구독 환불 처리
        subscription.refund();

        // 3. 포인트 반환 (FREE가 아닐 때)
        if (price > 0) {
            Wallet wallet = walletRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("지갑을 찾을 수 없습니다."));
            wallet.chargeBalance(price);

            // 4. PointHistory 기록
            PointHistory history = PointHistory.create(
                    wallet.getId(),
                    PointType.PAID,
                    PointHistoryType.REFUND,
                    price,
                    wallet.getBalance(),
                    "구독 환불 (" + subscription.getPlanType().name() + ")"
            );
            pointHistoryRepository.save(history);
        }

        return SubscriptionResponse.from(subscription);
    }

    /**
     * 구독 취소 - 다음 결제 중단, 만료일까지 이용 가능, 포인트 반환 없음
     */
    @Transactional
    public void cancel(Long userId) {
        Subscription subscription = subscriptionRepository
                .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("활성 중인 구독이 없습니다."));

        if (!subscription.getUserId().equals(userId)) {
            throw new IllegalStateException("본인의 구독만 취소할 수 있습니다.");
        }

        subscription.cancel();
    }
}