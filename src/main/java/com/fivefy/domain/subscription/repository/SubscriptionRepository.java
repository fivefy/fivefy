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
}