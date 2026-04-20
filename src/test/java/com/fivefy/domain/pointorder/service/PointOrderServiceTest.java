package com.fivefy.domain.pointorder.service;

import com.fivefy.domain.pointorder.dto.PointOrderPurchaseRequest;
import com.fivefy.domain.subscription.enums.SubscriptionPlanType;
import com.fivefy.domain.wallet.entity.Wallet;
import com.fivefy.domain.wallet.repository.WalletRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
class PointOrderServiceTest {

    @Autowired PointOrderService pointOrderService;
    @Autowired WalletRepository walletRepository;

    // 테스트용 고정 userId (실제 User 없이 지갑만 생성)
    private static final Long USER_ID = 99999L;
    private static final Long INITIAL_BALANCE = 200L;  // 200P
    private static final Long MONTH_PRICE = 50L;       // MONTH 플랜 가격

    @BeforeEach
    void setUp() {
        // 기존 지갑 삭제 후 새로 생성
        walletRepository.findByUserId(USER_ID)
                .ifPresent(walletRepository::delete);

        Wallet wallet = Wallet.create(USER_ID);
        wallet.chargeBalance(INITIAL_BALANCE);  // 200P 충전
        walletRepository.save(wallet);
    }

    @AfterEach
    void tearDown() {
        walletRepository.findByUserId(USER_ID)
                .ifPresent(walletRepository::delete);
    }

    @Test
    @DisplayName("동시 5건 구매 요청 — 200P / 50P = 최대 4건만 성공해야 함")
    void 동시_구독_구매_최대_성공_건수_검증() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();  // 모든 스레드가 준비될 때까지 대기 후 동시 시작
                    pointOrderService.purchase(
                        USER_ID,
                        new PointOrderPurchaseRequest(SubscriptionPlanType.MONTH)
                    );
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(15, java.util.concurrent.TimeUnit.SECONDS);

        Wallet wallet = walletRepository.findByUserId(USER_ID).orElseThrow();

        System.out.println("✅ 성공: " + successCount.get());
        System.out.println("❌ 실패: " + failCount.get());
        System.out.println("💰 최종 잔액: " + wallet.getBalance());

        // 200P / 50P = 4건 성공
        assertThat(successCount.get()).isEqualTo(4);
        assertThat(failCount.get()).isEqualTo(1);
        // 잔액이 절대 음수가 되면 안 됨 — 동시성 제어 핵심 검증
        assertThat(wallet.getBalance()).isGreaterThanOrEqualTo(0L);
        // 성공 건수 * 가격 = 차감 금액 정확성 검증
        assertThat(wallet.getBalance())
            .isEqualTo(INITIAL_BALANCE - (successCount.get() * MONTH_PRICE));
    }

    @Test
    @DisplayName("동시 10건 요청 — 잔액이 절대 음수가 되면 안 됨")
    void 동시_구독_구매_잔액_음수_방지_검증() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();  // 신호 대기
                    pointOrderService.purchase(
                        USER_ID,
                        new PointOrderPurchaseRequest(SubscriptionPlanType.MONTH)
                    );
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 잔액 부족 또는 락 타임아웃 — 정상 실패
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();  // 전체 동시 시작
        doneLatch.await();

        Wallet wallet = walletRepository.findByUserId(USER_ID).orElseThrow();

        System.out.println("✅ 성공: " + successCount.get());
        System.out.println("💰 최종 잔액: " + wallet.getBalance());

        // 핵심: 잔액이 절대 음수가 되면 안 됨
        assertThat(wallet.getBalance()).isGreaterThanOrEqualTo(0L);
        // 차감 금액 정확성
        assertThat(wallet.getBalance())
            .isEqualTo(INITIAL_BALANCE - (successCount.get() * MONTH_PRICE));
    }
}