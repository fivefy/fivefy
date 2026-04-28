package com.fivefy.domain.subscription.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.pointorder.entity.PointOrder;
import com.fivefy.domain.pointorder.repository.PointOrderRepository;
import com.fivefy.domain.subscription.dto.SubscriptionResponse;
import com.fivefy.domain.subscription.entity.Subscription;
import com.fivefy.domain.subscription.enums.SubscriptionErrorCode;
import com.fivefy.domain.subscription.enums.SubscriptionPlanType;
import com.fivefy.domain.subscription.enums.SubscriptionStatus;
import com.fivefy.domain.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PointOrderRepository pointOrderRepository;

    /**
     * 내 구독 조회
     * userId로 PointOrder 조회
     * pointOrderId로 Subscription 조회
     * @param userId
     * @return
     */
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getMySubscriptions(Long userId) {
        return subscriptionRepository.findAllByUserId(userId).stream()
                .map(SubscriptionResponse::from)
                .toList();
    }

    /**
     * 정기 구독 취소
     * 포인트 반환 없음
     * nextBillingDate → null, status → CANCELED
     * 만료일(expiryDate)까지는 이용 가능
     * @param userId
     */
    @Transactional
    public void cancel(Long userId) {
        Subscription subscription = subscriptionRepository
                .findByUserIdAndPlanTypeAndStatus(
                        userId,
                        SubscriptionPlanType.RECURRING,
                        SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.ERR_SUBSCRIPTION_NOT_FOUND));

        subscription.cancel();
    }
}