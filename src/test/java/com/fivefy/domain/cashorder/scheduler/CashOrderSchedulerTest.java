package com.fivefy.domain.cashorder.scheduler;

import com.fivefy.domain.billingkey.repository.BillingKeyRepository;
import com.fivefy.domain.cashorder.service.CashOrderService;
import com.fivefy.domain.pointorder.repository.PointOrderRepository;
import com.fivefy.domain.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashOrderSchedulerTest {
    @Mock
    private BillingKeyRepository billingKeyRepository;

    @Mock
    private CashOrderService cashOrderService;

    @InjectMocks
    private CashOrderScheduler cashOrderScheduler;

    @Test
    void 자동충전스케줄러는_nextChargeDate가_지난_빌링키만_조회한다() {
        when(billingKeyRepository.findAllByActiveTrueAndNextChargeDateLessThanEqual(any()))
                .thenReturn(List.of());

        cashOrderScheduler.processRecurringCharges();

        verify(billingKeyRepository)
                .findAllByActiveTrueAndNextChargeDateLessThanEqual(any(LocalDateTime.class));

        verifyNoInteractions(cashOrderService);
    }
}