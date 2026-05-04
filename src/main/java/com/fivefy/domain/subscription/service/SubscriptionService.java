package com.fivefy.domain.subscription.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.notification.enums.NotificationType;
import com.fivefy.domain.notification.event.NotificationEvent;
import com.fivefy.domain.pointorder.repository.PointOrderRepository;
import com.fivefy.domain.subscription.dto.SubscriptionResponse;
import com.fivefy.domain.subscription.entity.Subscription;
import com.fivefy.domain.subscription.enums.SubscriptionErrorCode;
import com.fivefy.domain.subscription.enums.SubscriptionPlanType;
import com.fivefy.domain.subscription.enums.SubscriptionStatus;
import com.fivefy.domain.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PointOrderRepository pointOrderRepository;
    private final ApplicationEventPublisher eventPublisher;

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
     * 구독 취소 : 활성화 -> 취소 : 정기 구독 취소 상태. 만료일이 되면 만료 상태로 변경
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

        eventPublisher.publishEvent(NotificationEvent.of(
                subscription.getUserId(),
                NotificationType.SUBSCRIPTION_CANCEL,
                "구독 취소 성공"
        ));
    }

    // 건별 만료 처리 — 스케줄러에서 호출
    // 기존의 엔티티 안의 매서드 expire() 기능을 트랜젝션 안에 넣어서 실행
    @Transactional
    public void expireOne(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.ERR_SUBSCRIPTION_NOT_FOUND));
        subscription.expire();
    }
}