package com.fivefy.domain.cashorder.service;

import com.fivefy.domain.cashorder.dto.CashOrderVerifyRequest;
import com.fivefy.domain.cashorder.enums.CashProductType;
import com.fivefy.domain.cashorder.repository.CashOrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CashOrderServiceTest {

    @Autowired CashOrderService cashOrderService;
    @Autowired CashOrderRepository cashOrderRepository;

    private static final Long USER_ID = 99998L;

    @AfterEach
    void tearDown() {
        cashOrderRepository.deleteAll(
            cashOrderRepository.findAll().stream()
                .filter(o -> o.getUserId().equals(USER_ID))
                .toList()
        );
    }

    @Test
    @DisplayName("동시 5건 충전 주문 생성 — 각각 고유한 orderNumber를 가져야 함")
    void 동시_충전_주문_생성_중복_방지_검증() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    cashOrderService.purchase(
                        USER_ID,
                        new CashOrderVerifyRequest(CashProductType.PRODUCT_1)
                    );
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 락 타임아웃 시 실패 가능
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();

        System.out.println("성공: " + successCount.get());

        // 생성된 주문의 orderNumber가 모두 고유한지 확인
        long distinctOrderCount = cashOrderRepository.findAll().stream()
            .filter(o -> o.getUserId().equals(USER_ID))
            .map(o -> o.getOrderNumber())
            .distinct()
            .count();

        assertThat(distinctOrderCount).isEqualTo(successCount.get());
    }
}