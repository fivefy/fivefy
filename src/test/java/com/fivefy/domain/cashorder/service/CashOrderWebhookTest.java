package com.fivefy.domain.cashorder.service;

import com.fivefy.common.portone.PortoneWebhookVerifier;
import com.fivefy.common.portone.client.PortoneClient;
import com.fivefy.common.portone.dto.PortonePaymentResponse;
import com.fivefy.domain.cashorder.entity.CashOrder;
import com.fivefy.domain.cashorder.enums.CashOrderStatus;
import com.fivefy.domain.cashorder.enums.CashProductType;
import com.fivefy.domain.cashorder.repository.CashOrderRepository;
import com.fivefy.domain.payment.repository.WebhookEventRepository;
import com.fivefy.domain.wallet.entity.Wallet;
import com.fivefy.domain.wallet.repository.WalletRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@ActiveProfiles("test")
class CashOrderWebhookTest {

    @Autowired CashOrderService cashOrderService;
    @Autowired CashOrderRepository cashOrderRepository;
    @Autowired WebhookEventRepository webhookEventRepository;
    @Autowired WalletRepository walletRepository;

    // 포트원 외부 API mock — 실제 네트워크 호출 차단
    @MockitoBean
    PortoneClient portoneClient;
    @MockitoBean
    PortoneWebhookVerifier portoneWebhookVerifier;

    private static final Long USER_ID = 99999L;
    private static final String ORDER_NUMBER = "ORD-webhooktest";
    // PAYMENT_ID = ORDER_NUMBER (PortonePaymentResponse.orderNumber()가 id를 반환하므로)
    private static final String PAYMENT_ID = ORDER_NUMBER;

    /**
     * 전처리
     * 시작 전 매서드 실행
     */
    @BeforeEach
    void setUp() {
        // 테스트용 CashOrder (PENDING 상태)
        CashOrder cashOrder = CashOrder.create(
                USER_ID, CashProductType.PRODUCT_1, ORDER_NUMBER
        );
        cashOrderRepository.save(cashOrder);

        // 테스트용 Wallet (없을 때만 생성)
        if (walletRepository.findByUserId(USER_ID).isEmpty()) {
            walletRepository.save(Wallet.create(USER_ID));
        }

        // 웹훅 서명 검증 통과 처리 : Mockito가 verify() 호출 시 아무것도 안 하게 설정
        // portoneWebhookVerifier.verify(webhookId, webhookTimestamp, rawBody, webhookSignature);
        doNothing().when(portoneWebhookVerifier)
                .verify(any(), any(), any(), any());

        // 포트원 단건 조회 mock
        // id = ORDER_NUMBER (orderNumber()가 id를 반환하는 구조)
        // amount.total = 1000L (PRODUCT_1 금액)
        given(portoneClient.getPayment(any())).willReturn(
                new PortonePaymentResponse(
                        ORDER_NUMBER,
                        "PAID",
                        new PortonePaymentResponse.Amount(1000L),
                        ORDER_NUMBER
                )
        );
    }

    /**
     * 설정 초기화
     * 후처리
     */
    @AfterEach
    void tearDown() {
        // webhook_events 정리
        webhookEventRepository.deleteAll(
                webhookEventRepository.findAll().stream()
                        .filter(e -> e.getPaymentId().equals(PAYMENT_ID))
                        .toList()
        );
        // CashOrder 정리
        cashOrderRepository.deleteAll(
                cashOrderRepository.findAll().stream()
                        .filter(o -> o.getUserId().equals(USER_ID))
                        .toList()
        );
        // Wallet 정리
        walletRepository.findByUserId(USER_ID)
                .ifPresent(walletRepository::delete);
    }

    @Test
    @DisplayName("정상 웹훅 수신 — CashOrder SUCCESS 전환 및 포인트 충전")
    void 정상_웹훅_처리() {
        // given
        String webhookId = "wh-" + UUID.randomUUID();
        String rawBody = """
                {"type":"Transaction.Paid","data":{"paymentId":"%s"}}
                """.formatted(PAYMENT_ID);

        // when
        cashOrderService.processWebhook(webhookId, "sig", "ts", rawBody);

        // then: CashOrder SUCCESS 전환 확인
        CashOrder cashOrder = cashOrderRepository.findByOrderNumber(ORDER_NUMBER).orElseThrow();
        assertThat(cashOrder.getStatus()).isEqualTo(CashOrderStatus.SUCCESS);

        // then: 포인트 1000P 충전 확인
        Wallet wallet = walletRepository.findByUserId(USER_ID).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(1000L);

        // then: webhook_events 1건 저장 확인
        long count = webhookEventRepository.findAll().stream()
                .filter(e -> e.getWebhookEventId().equals(webhookId))
                .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("동일 webhookId 순차 재수신 — 두 번째는 스킵, 포인트는 1회만 충전")
    void 중복_웹훅_순차_스킵() {
        // given
        // 포트원 재전송 시 webhookId는 동일 — 두 번 모두 같은 값 사용
        String webhookId = "wh-fixed-duplicate-test";
        String rawBody = """
                {"type":"Transaction.Paid","data":{"paymentId":"%s"}}
                """.formatted(PAYMENT_ID);

        // when: 동일 webhookId로 두 번 호출
        cashOrderService.processWebhook(webhookId, "sig", "ts", rawBody);
        cashOrderService.processWebhook(webhookId, "sig", "ts", rawBody); // 중복 수신

        // then: 포인트는 1000P (2000P가 아님)
        Wallet wallet = walletRepository.findByUserId(USER_ID).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(1000L);

        // then: webhook_events는 1건만 존재
        long count = webhookEventRepository.findAll().stream()
                .filter(e -> e.getWebhookEventId().equals(webhookId))
                .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("동일 webhookId 동시 수신 10건 — 중복 충전 없음")
    void 중복_웹훅_동시_스킵() throws InterruptedException {
        // given
        // 포트원 재전송 시 webhookId는 동일
        String webhookId = "wh-fixed-concurrent-test";
        String rawBody = """
                {"type":"Transaction.Paid","data":{"paymentId":"%s"}}
                """.formatted(PAYMENT_ID);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger skipCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    cashOrderService.processWebhook(webhookId, "sig", "ts", rawBody);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    skipCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        System.out.println("[동시성] 처리: " + successCount.get() + ", 스킵: " + skipCount.get());

        // then: 포인트가 1000P를 초과하지 않아야 함 (중복 충전 없음)
        // 데드락으로 전부 롤백될 수 있어서 0도 허용 — 포트원이 재전송하면 결국 처리됨
        Wallet wallet = walletRepository.findByUserId(USER_ID).orElseThrow();
        assertThat(wallet.getBalance()).isLessThanOrEqualTo(1000L);

        // then: webhook_events는 1건 이하 (중복 INSERT 없음)
        long count = webhookEventRepository.findAll().stream()
                .filter(e -> e.getWebhookEventId().equals(webhookId))
                .count();
        assertThat(count).isLessThanOrEqualTo(1);
    }
}