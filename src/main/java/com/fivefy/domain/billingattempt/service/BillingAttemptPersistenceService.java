package com.fivefy.domain.billingattempt.service;

import com.fivefy.domain.billingattempt.entity.BillingAttempt;
import com.fivefy.domain.billingattempt.enums.BillingFailureReason;
import com.fivefy.domain.billingattempt.repository.BillingAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingAttemptPersistenceService {

    private final BillingAttemptRepository billingAttemptRepository;

    /**
     * 실패 기록
     * REQUIRES_NEW — 부모 트랜잭션 롤백과 무관하게 반드시 커밋
     * "이번 달에 이미 실패했다"는 기록이 남아야 재시도 중복 방지가 가능
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailure(Long subscriptionId, Long billingKeyId,
                            BillingFailureReason reason) {
        BillingAttempt attempt = BillingAttempt.failure(subscriptionId, billingKeyId, reason);
        billingAttemptRepository.save(attempt);
        log.info("[BillingAttempt] 실패 기록 subscriptionId={}, reason={}", subscriptionId, reason);
    }

    /**
     * 성공 기록
     * saveRecurringChargeResult() 내부 트랜잭션에서 같이 커밋
     * 별도 REQUIRES_NEW 불필요 — CashOrder/Payment/PointHistory와 원자적으로 저장
     */
    @Transactional
    public void saveSuccess(Long subscriptionId, Long billingKeyId) {
        BillingAttempt attempt = BillingAttempt.success(subscriptionId, billingKeyId);
        billingAttemptRepository.save(attempt);
        log.info("[BillingAttempt] 성공 기록 subscriptionId={}", subscriptionId);
    }
}