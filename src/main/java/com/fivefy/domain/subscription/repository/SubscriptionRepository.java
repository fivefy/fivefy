package com.fivefy.domain.subscription.repository;

import com.fivefy.domain.subscription.entity.Subscription;
import com.fivefy.domain.subscription.enums.SubscriptionPlanType;
import com.fivefy.domain.subscription.enums.SubscriptionStatus;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // pointOrderId 목록으로 구독 전체 조회 (내 구독 목록)
    List<Subscription> findAllByPointOrderIdIn(List<Long> pointOrderIds);

    // pointOrderId 단건으로 구독 조회
    Optional<Subscription> findByPointOrderId(Long pointOrderId);

    // 구독 상태 확인 — pointOrderId 목록 + 상태 필터
    @Query("""
        SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
        FROM Subscription s
        WHERE s.pointOrderId IN :pointOrderIds
          AND s.status IN :statuses
        """)
    boolean existsByPointOrderIdInAndStatusIn(
            @Param("pointOrderIds") List<Long> pointOrderIds,
            @Param("statuses") List<SubscriptionStatus> statuses
    );

    // pointOrderId 목록 + planType + 상태로 구독 단건 조회
    // 빌링키 사용자가 현재 활성 정기 구독을 가지고 있는지 확인
    @Query("""
        SELECT s FROM Subscription s
        WHERE s.pointOrderId IN :pointOrderIds
          AND s.planType = :planType
          AND s.status = :status
        """)
    Optional<Subscription> findByPointOrderIdInAndPlanTypeAndStatus(
            @Param("pointOrderIds") List<Long> pointOrderIds,
            @Param("planType") SubscriptionPlanType planType,
            @Param("status") SubscriptionStatus status
    );

    // 정기 구독 자동 결제 스케줄러용 — 결제일이 된 RECURRING 구독 조회
    List<Subscription> findAllByPlanTypeAndStatusAndNextBillingDateBefore(
            SubscriptionPlanType planType,
            SubscriptionStatus status,
            LocalDateTime dateTime
    );

    // 구독 만료 스케줄러용 — 만료일이 지난 활성화 구독 조회
    // (dataTime -> now) ,활성화와 비활성화 둘 모두 체크했으나, 비활성화는 고려할 필요도 없다)
    List<Subscription> findAllByStatusInAndExpiryDateBefore(
            // SubscriptionStatus status,
            List<SubscriptionStatus> statuses,
            LocalDateTime now
    );

    @Query("""
            select s
            from Subscription s
            join PointOrder po on po.id = s.pointOrderId
            where po.userId = :userId
              and s.planType = :planType
              and s.status = :status
              and s.nextBillingDate is not null
            """)
    Optional<Subscription> findByUserIdAndPlanTypeAndStatus(
            @Param("userId") Long userId,
            @Param("planType") SubscriptionPlanType planType,
            @Param("status") SubscriptionStatus status
    );

    // 테스트 정리용
    void deleteAllByPointOrderIdIn(List<Long> pointOrderIds);

    /**
     * 코드래빗 조언 : 조건을 만족하는 행이 하나라도 있으면 즉시 멈추고 true 반환
     * @param pointOrderIds
     * @param status
     * @return
     */
    boolean existsByPointOrderIdInAndStatus(
            List<Long> pointOrderIds,
            SubscriptionStatus status
    );
}