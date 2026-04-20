package com.fivefy.domain.cashorder.service;

import com.fivefy.common.portone.PortoneWebhookVerifier;
import com.fivefy.common.portone.client.PortoneClient;
import com.fivefy.common.portone.dto.PortoneCancelResponse;
import com.fivefy.common.portone.dto.PortonePaymentResponse;
import com.fivefy.common.portone.dto.PortoneWebhookRequest;
import com.fivefy.domain.cashorder.dto.CashOrderPurchaseResponse;
import com.fivefy.domain.cashorder.dto.CashOrderRefundRequest;
import com.fivefy.domain.cashorder.dto.CashOrderResponse;
import com.fivefy.domain.cashorder.dto.CashOrderVerifyRequest;
import com.fivefy.domain.cashorder.entity.CashOrder;
import com.fivefy.domain.cashorder.enums.CashOrderStatus;
import com.fivefy.domain.cashorder.enums.CashProductType;
import com.fivefy.domain.cashorder.repository.CashOrderRepository;
import com.fivefy.domain.payment.entity.Payment;
import com.fivefy.domain.payment.repository.PaymentRepository;
import com.fivefy.domain.wallet.entity.PointHistory;
import com.fivefy.domain.wallet.entity.Wallet;
import com.fivefy.domain.wallet.enums.PointHistoryType;
import com.fivefy.domain.wallet.enums.PointType;
import com.fivefy.domain.wallet.repository.PointHistoryRepository;
import com.fivefy.domain.wallet.repository.WalletRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CashOrderService {

    private final CashOrderRepository cashOrderRepository;
    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final PortoneClient portoneClient;
    private final PortoneWebhookVerifier portoneWebhookVerifier;
    private final ObjectMapper objectMapper;    // Spring Boot가 기본으로 Bean 등록


    /**
     * 포인트 충전 주문
     * PENDING 상태 CashOrder 저장 후 orderNumber만 반환
     * 프론트가 orderNumber를 포트원 SDK에 넘겨 결제창 호출
     * 실제 포인트 충전은 웹훅 수신 후 processWebhook()에서 처리
     *
     * orderNumber  : 생성 ("ORD-" + UUID 앞 8자리)
     * cashOrder    : 생성 (PENDING) → 저장
     * orderNumber  : 반환 → 프론트 → 포트원 SDK 결제창 호출
     *              : ★ orderNumber는 웹훅에선 merchandUid라고도 한다.(이걸로 연결)
     * @param userId
     * @param request
     * @return
     */
    @Transactional
    public CashOrderPurchaseResponse purchase(Long userId, CashOrderVerifyRequest request) {

        CashProductType productType = request.productType();

        // 포트원 SDK에 넘길 주문 식별자 (포트원이 이 값을 paymentId로 사용)
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8);

        // PENDING 상태로 저장 — webhookId는 웹훅 수신 시 채워짐
        CashOrder cashOrder = CashOrder.create(userId, productType, orderNumber);
        cashOrderRepository.save(cashOrder);

        //  orderNumber만 반환 (결제 완료는 웹훅이 처리)
        return new CashOrderPurchaseResponse(orderNumber);
    }

    /**
     * 포인트 환불
     * 포트원 취소 API 호출 후 DB 상태 변경 및 포인트 차감
     *
     * CashOrder 조회 + 본인 검증
     * Payment 조회 → pgTransactionId(포트원 결제 ID) 확보
     * 포트원 취소 API 호출 → SUCCEEDED 확인
     * CashOrder.refund() + Payment.refund() 상태 변경
     * wallet.useBalance() → 포인트 차감 + PointHistory(REFUND) 기록
     * @param userId
     * @param request
     * @return
     */
    @Transactional
    public CashOrderResponse refund(Long userId, CashOrderRefundRequest request) {

        // 주문 조회
        CashOrder cashOrder = cashOrderRepository.findByOrderNumber(request.orderNumber())
                .orElseThrow(() -> new EntityNotFoundException("없는 주문"));

        // 본인 검증
        if (!cashOrder.getUserId().equals(userId))
            throw new IllegalArgumentException("본인 주문만 환불 가능");

        // Payment(CashOrder의 기록, pg트랜젝션과 멱등키가 있다.)에서 pgTransactionId 가져오기
        // Payment 상태 변경 (orderNumber로 조회 : 상태변경) : 포인트 구매 이력을 조회해서 orderNumber을 통해 환불(PaymentResponse)
        Payment payment = paymentRepository.findByOrderNumber(request.orderNumber())
             .orElseThrow(() -> new EntityNotFoundException("결제 내역 없음"));

        // 포트원에 취소 요청 (참고 코드의 cancelPaymentByImpUid에 해당)
        PortoneCancelResponse cancelResponse = portoneClient.cancelPayment(
            payment.getPgTransactionId(),
            cashOrder.getCashAmount(),
            request.reason()
        );

        // 취소 응답 확인 : SUCCEEDED가 아니면 처리 중단
        if (!"SUCCEEDED".equals(cancelResponse.status())) {
            throw new IllegalStateException("포트원 결제 취소 실패: " + cancelResponse.status());
        }

        // DB 상태 변경
        cashOrder.refund();                 // CashOrder.status: SUCCESS → REFUNDED
        payment.refund(request.reason());   // Payment.status: COMPLETED → REFUNDED, 환불이유/환불시간(refundReason/refundedAt) 기록

        // 지갑의 포인트 차감
        // 포인트를 이미 사용한 경우 useBalance()에서 잔액 부족 예외 발생 가능
        //   → 스케줄러 도입 시 음수 잔액 허용 또는 별도 정책 필요
        Wallet wallet = walletRepository.findByUserId(cashOrder.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("지갑 없음"));
        wallet.useBalance(cashOrder.getPointAmount());
        // 지갑 내역(PointHistory)
        pointHistoryRepository.save(PointHistory.create(
                wallet.getId(),
                PointType.PAID,
                PointHistoryType.REFUND,
                cashOrder.getPointAmount(),
                wallet.getBalance(),    // 잔액 스냅샷
                "포인트 환불 (" + request.reason() + ")"
        ));

        return CashOrderResponse.from(cashOrder);
    }

    /**
     * 포트원 웹훅 처리 (결제 완료 시 포트원 서버가 호출)
     * -PortoneWebhookController에서 헤더 검증 후 이 메서드로 위임
     *
     * rawBody 파싱 → 웹훅 타입 확인 (Transaction.Paid만 처리)
     * 시그니처 + 타임스탬프 검증 (위변조 / 리플레이 방지)
     * webhookId 중복 체크 (멱등성 — 포트원은 웹훅을 재전송할 수 있음)
     *
     * 포트원 단건 조회 → 금액 검증
     * CashOrder.success(webhookId) + Payment 생성
     * wallet.chargeBalance() + PointHistory(CHARGE) 기록
     * @param webhookId
     * @param webhookSignature
     * @param webhookTimestamp
     * @param rawBody
     */
    @Transactional
    public void processWebhook(
            String webhookId,
            String webhookSignature,
            String webhookTimestamp,
            String rawBody
    ) {
        // 웹훅 수신 즉시 로그(디버깅용)
        System.out.println("=== 웹훅 수신 ===");
        System.out.println("webhookId: " + webhookId);
        System.out.println("rawBody: " + rawBody);
        System.out.println("=================");

        // 웹훅 body 파싱 : rawBody → PortoneWebhookRequest
        PortoneWebhookRequest request;
        try {
            request = objectMapper.readValue(rawBody, PortoneWebhookRequest.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("웹훅 body 파싱 실패");
        }

        System.out.println("웹훅 타입: " + request.type());

        // 구독(포인트 소모) 결제 승인(Transaction.Paid)만 처리, 나머지 타입은 무시
        // 구독 결제 취소(Transaction.Cancelled) 등 다른 타입은 여기서 걸러짐
        if (!"Transaction.Paid".equals(request.type())) {
            return;
        }
        // 시그니처 + 타임스탬프 검증 (위변조 / 리플레이 방지)
        portoneWebhookVerifier.verify(webhookId, webhookTimestamp, rawBody, webhookSignature);

        // webhook-id 중복 체크 (멱등성 보장, 이미 처리된 웹훅이면 즉시 종료)
        if (cashOrderRepository.existsByWebhookId(webhookId)) {
            return;
        }

        // 포트원 단건 조회
        String pgPaymentId = request.data().paymentId();  // ORD-xxxxxx : 포트원이 부여한 결제 ID (= orderNumber)

        //                                  실제 결제 데이터 확인
        PortonePaymentResponse pgPayment = portoneClient.getPayment(pgPaymentId);

        // CashOrder 조회 (orderNumber 기준)
        CashOrder cashOrder = cashOrderRepository.findByOrderNumber(pgPayment.orderNumber())
                .orElseThrow(() -> new IllegalArgumentException("주문 없음"));


        // 상태(결제 대기) : PENDING이 아니면 이미 처리된 주문 → 무시
        if (cashOrder.getStatus() != CashOrderStatus.PENDING) {
            return;
        }
        
        // 금액 검증
        // PortonePaymentResponse에서 결제 대금을 totalAmount으로 두고, 포인트 구매에서 결제 대금을 CashAmount로 둔다.
        if (!pgPayment.totalAmount().equals(cashOrder.getCashAmount())) {
            throw new IllegalStateException("결제 금액 불일치");
        }

        // CashOrder PENDING → SUCCESS, webhookId 저장
        cashOrder.success(webhookId);

        // Payment 기록
        Payment payment = Payment.create(
                cashOrder.getUserId(),
                cashOrder.getCashAmount(),
                cashOrder.getOrderNumber(),
                pgPaymentId,   // pgTransactionId — 포트원 결제 ID(환불 시 필요)
                webhookId      // webhookId (=idempotencyKey, 멱등키) — 포트원이 준 웹훅 고유 ID
        );
        payment.complete();
        paymentRepository.save(payment);

        // Wallet 충전
        Wallet wallet = walletRepository.findByUserId(cashOrder.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("지갑 없음"));
        wallet.chargeBalance(cashOrder.getPointAmount());   // 포인트 지급
        // 지갑 이력(PointHistory)
        pointHistoryRepository.save(PointHistory.create(
                wallet.getId(),
                PointType.PAID,
                PointHistoryType.CHARGE,
                cashOrder.getPointAmount(),
                wallet.getBalance(),        // 충전 후 잔액 스냅샷
            "포인트 충전 (" + cashOrder.getProductType().getDescription() + ")"
        ));


    }
}