package com.fivefy.domain.pointorder.repository;

import com.fivefy.domain.pointorder.entity.PointOrder;
import com.fivefy.domain.subscription.enums.SubscriptionPlanType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PointOrderRepository extends JpaRepository<PointOrder, Long> {

    Optional<PointOrder> findByOrderNumber(String orderNumber);

    List<PointOrder> findAllByUserId(Long userId);

    // FREE 플랜 중복 사용 방지 체크용
    boolean existsByUserIdAndPlanType(Long userId, SubscriptionPlanType planType);
}
