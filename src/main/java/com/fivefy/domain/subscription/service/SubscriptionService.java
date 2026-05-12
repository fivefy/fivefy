package com.fivefy.domain.subscription.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.notification.entity.NotificationOutbox;
import com.fivefy.domain.notification.enums.NotificationType;
import com.fivefy.domain.notification.repository.NotificationOutboxRepository;
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
    private final NotificationOutboxRepository outboxRepository;

    /**
     * 내 구독 조회
     * userId로 PointOrder 조회
     * pointOrderId로 Subscription 조회
     *
     * @param userId
     * @return
     */
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getMySubscriptions(Long userId) {
        // 1. userId로 PointOrder 목록 조회
        List<Long> pointOrderIds = pointOrderRepository.findAllByUserId(userId)
                .stream()
                .map(PointOrder::getId)
                .toList();

        // 2. pointOrderId 목록으로 구독 조회
        return subscriptionRepository.findAllByPointOrderIdIn(pointOrderIds)
                .stream()
                .map(SubscriptionResponse::from)
                .toList();
    }

    /**
     * 정기 구독 취소
     * 포인트 반환 없음
     * nextBillingDate → null, status → CANCELED
     * 만료일(expiryDate)까지는 이용 가능
     * 구독 취소 : 활성화 -> 취소 : 정기 구독 취소 상태. 만료일이 되면 만료 상태로 변경
     *
     * userId → pointOrderId 목록 → RECURRING + ACTIVE 구독 조회
     * @param userId
     */
    @Transactional
    public void cancel(Long userId) {
        // 1. userId로 PointOrder 목록 조회
        List<Long> pointOrderIds = pointOrderRepository.findAllByUserId(userId)
                .stream()
                .map(PointOrder::getId)
                .toList();

        // 2. pointOrderId 목록으로 RECURRING + ACTIVE 구독 조회
        Subscription subscription = subscriptionRepository
                .findByPointOrderIdInAndPlanTypeAndStatus(
                        pointOrderIds,
                        SubscriptionPlanType.RECURRING,
                        SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        SubscriptionErrorCode.ERR_SUBSCRIPTION_NOT_FOUND));

        subscription.cancel();

        outboxRepository.save(NotificationOutbox.create(
                NotificationType.SUBSCRIPTION_CANCEL,
                userId,                          // 알림 수신자는 userId 필요 — PointOrder에서 조회
                null,
                subscription.getId(),
                "구독 취소 성공"
        ));
    }

    /**
     * 건별 만료 처리 — 스케줄러에서 호출
     * 기존의 엔티티 안의 매서드 expire() 기능을 트랜젝션 안에 넣어서 실행
     *
     * userId는 pointOrderId를 통해 역추적
     * @param subscriptionId
     */
    @Transactional
    public void expireOne(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new BusinessException(
                        SubscriptionErrorCode.ERR_SUBSCRIPTION_NOT_FOUND));
        subscription.expire();

        // 알림 수신자 userId — PointOrder를 통해 역추적
        Long userId = getUserIdFromSubscription(subscription);

        outboxRepository.save(NotificationOutbox.create(
                NotificationType.SUBSCRIPTION_EXPIRE,
                userId,
                null,
                subscription.getId(),
                "구독이 만료되었습니다."
        ));
    }


    /**
     * userId → pointOrderId 목록
     *
     * @param userId
     * @return
     */
    private List<Long> getPointOrderIds(Long userId) {
        return pointOrderRepository.findAllByUserId(userId)
                .stream()
                .map(PointOrder::getId)
                .toList();
    }

    /**
     * userId → RECURRING + ACTIVE 구독 단건 조회
     *
     * @param userId
     * @return
     */
    private Subscription findActiveRecurringSubscription(Long userId) {
        List<Long> pointOrderIds = getPointOrderIds(userId);

        return subscriptionRepository.findAllByPointOrderIdIn(pointOrderIds)
                .stream()
                .filter(s -> s.getPlanType() == SubscriptionPlanType.RECURRING
                        && s.getStatus() == SubscriptionStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        SubscriptionErrorCode.ERR_SUBSCRIPTION_NOT_FOUND));
    }

    /**
     * Subscription → PointOrder → userId 역추적
     *
     * @param subscription
     * @return
     */
    private Long getUserIdFromSubscription(Subscription subscription) {
        return pointOrderRepository.findById(subscription.getPointOrderId())
                .map(PointOrder::getUserId)
                .orElseThrow(() -> new BusinessException(
                        SubscriptionErrorCode.ERR_SUBSCRIPTION_NOT_FOUND));
    }

    /**
     * SubscriptionController의 기능 옮김
     *
     * @param userId
     * @return
     */
    public Subscription findActiveRecurringAutoSubscription(Long userId) {
        List<Long> pointOrderIds = getPointOrderIds(userId);
        return subscriptionRepository.findAllByPointOrderIdIn(pointOrderIds)
                .stream()
                .filter(s -> s.getPlanType() == SubscriptionPlanType.RECURRING_AUTO
                          && s.getStatus() == SubscriptionStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        SubscriptionErrorCode.ERR_SUBSCRIPTION_RECURRING_NOT_FOUND));
    }

    /**
     * 구독 활성 여부만 boolean으로 반환
     *
     * @param userId
     * @return
     */
    public boolean isActiveSubscriber(Long userId) {
        // userId → pointOrderIds
        List<Long> pointOrderIds = pointOrderRepository.findAllByUserId(userId)
                .stream()
                .map(PointOrder::getId)
                .toList();

        // 구독 존재 여부(활성화, ACTIVE)만 확인
        return subscriptionRepository.existsByPointOrderIdInAndStatus(
                    pointOrderIds,
                    SubscriptionStatus.ACTIVE
        );
    }
}