package com.fivefy.domain.billingattempt.repository;

import com.fivefy.domain.billingattempt.entity.BillingAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingAttemptRepository extends JpaRepository<BillingAttempt, Long> {

    Optional<BillingAttempt> findBySubscriptionIdAndBillingCycle(
            Long subscriptionId, String billingCycle);
}