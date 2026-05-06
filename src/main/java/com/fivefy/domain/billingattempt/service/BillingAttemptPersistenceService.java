package com.fivefy.domain.billingattempt.service;

import com.fivefy.domain.billingattempt.entity.BillingAttempt;
import com.fivefy.domain.billingattempt.enums.BillingAttemptStatus;
import com.fivefy.domain.billingattempt.enums.BillingFailureReason;
import com.fivefy.domain.billingattempt.repository.BillingAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingAttemptPersistenceService {

    private final BillingAttemptRepository billingAttemptRepository;

    private static final DateTimeFormatter CYCLE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * 실패 기록
     * REQUIRES_NEW — 부모 트랜잭션 롤백과 무관하게 반드시 커밋
     * "이번 달에 이미 실패했다"는 기록이 남아야 재시도 중복 방지가 가능
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailure(Long userId, Long billingKeyId,
                            BillingFailureReason reason) {
        BillingAttempt attempt = BillingAttempt.failure(userId, billingKeyId, reason);
        billingAttemptRepository.save(attempt);
        log.info("[BillingAttempt] 실패 기록 userId={}, reason={}", userId, reason);
    }

    /**
     * 성공 기록
     * saveRecurringChargeResult() 내부 트랜잭션에서 같이 커밋
     * 별도 REQUIRES_NEW 불필요 — CashOrder/Payment/PointHistory와 원자적으로 저장
     */
    @Transactional
    public void saveSuccess(Long userId, Long billingKeyId) {
        String currentCycle = YearMonth.now().format(CYCLE_FORMATTER);

        // 이번 달 SUCCESS 기록이 이미 있으면 중복 저장 방지
        boolean alreadySuccess = billingAttemptRepository
                .findByUserIdAndBillingCycle(userId, currentCycle)
                .stream()
                .anyMatch(a -> a.getStatus() == BillingAttemptStatus.SUCCESS);

        if (alreadySuccess) {
            // 이미 이번 달 성공했으면 또 저장하지 않음
            log.warn("[BillingAttempt] 이미 성공 기록 존재 userId={}, cycle={}",
                    userId, currentCycle);
            return;
        }

        billingAttemptRepository.save(BillingAttempt.success(userId, billingKeyId));
        log.info("[BillingAttempt] 성공 기록 userId={}", userId);
    }
}