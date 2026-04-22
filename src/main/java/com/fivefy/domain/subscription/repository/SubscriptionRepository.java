package com.fivefy.domain.subscription.repository;

import com.fivefy.domain.subscription.entity.Subscription;
import com.fivefy.domain.subscription.enums.SubscriptionPlanType;
import com.fivefy.domain.subscription.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByPointOrderId(Long pointOrderId);

    List<Subscription> findAllByPointOrderIdIn(List<Long> pointOrderIds);

    Optional<Subscription> findByUserIdAndPlanType(Long userId, SubscriptionPlanType planType);

    // 사용자 구독 상태가 체험(FREE) 또는 ACTIVE인지 확인하기 위한 존재 여부 조회
    boolean existsByUserIdAndStatusIn(Long userId, List<SubscriptionStatus> statuses);

    // 테스트 정리 코드
    void deleteAllByUserId(Long userId);

    // 정기 구독 자동 결제 스케줄러용 — 결제일이 된 RECURRING 구독 조회(미적용)(2026-04-20)
    List<Subscription> findAllByPlanTypeAndStatusAndNextBillingDateBefore(
            SubscriptionPlanType planType,
            SubscriptionStatus status,
            LocalDateTime dateTime
    );

    // 구독 만료 스케줄러용 — 만료일이 지난 구독 조회(미적용)(2026-04-20)
    List<Subscription> findAllByStatusInAndExpiryDateBefore(
            List<SubscriptionStatus> statuses,
            LocalDateTime dateTime
    );

    // [테스트 전용] 구독 포인트 차감 수동 실행
    Optional<Subscription> findByUserIdAndPlanTypeAndStatus(
            Long userId,
            SubscriptionPlanType planType,
            SubscriptionStatus status
    );
}