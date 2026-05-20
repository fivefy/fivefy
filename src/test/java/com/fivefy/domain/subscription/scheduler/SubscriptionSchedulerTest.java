package com.fivefy.domain.subscription.scheduler;

import com.fivefy.domain.pointorder.repository.PointOrderRepository;
import com.fivefy.domain.pointorder.service.PointOrderService;
import com.fivefy.domain.subscription.enums.SubscriptionPlanType;
import com.fivefy.domain.subscription.enums.SubscriptionStatus;
import com.fivefy.domain.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionSchedulerTest {
    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PointOrderRepository pointOrderRepository;

    @Mock
    private PointOrderService pointOrderService;

    @InjectMocks
    private SubscriptionScheduler subscriptionScheduler;
    @Test
    void 구독스케줄러는_nextBillingDate가_지난_RECURRING_AUTO_ACTIVE만_조회한다() {
        when(subscriptionRepository.findAllByPlanTypeAndStatusAndNextBillingDateBefore(
                eq(SubscriptionPlanType.RECURRING_AUTO),
                eq(SubscriptionStatus.ACTIVE),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        when(pointOrderRepository.findAllById(any()))
                .thenReturn(List.of());

        subscriptionScheduler.processRecurringPayments();

        verify(subscriptionRepository)
                .findAllByPlanTypeAndStatusAndNextBillingDateBefore(
                        eq(SubscriptionPlanType.RECURRING_AUTO),
                        eq(SubscriptionStatus.ACTIVE),
                        any(LocalDateTime.class)
                );
        verifyNoInteractions(pointOrderService);
    }
}