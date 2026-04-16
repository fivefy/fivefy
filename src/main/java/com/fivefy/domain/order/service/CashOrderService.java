package com.fivefy.domain.order.service;

import com.fivefy.common.portone.PortoneWebhookVerifier;
import com.fivefy.common.portone.client.PortoneClient;
import com.fivefy.common.portone.dto.PortoneCancelResponse;
import com.fivefy.common.portone.dto.PortonePaymentResponse;
import com.fivefy.common.portone.dto.PortoneWebhookRequest;
import com.fivefy.domain.order.dto.CashOrderPurchaseResponse;
import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.order.dto.CashOrderRefundRequest;
import com.fivefy.domain.order.dto.CashOrderResponse;
import com.fivefy.domain.order.dto.CashOrderVerifyRequest;
import com.fivefy.domain.order.entity.CashOrder;
import com.fivefy.domain.order.enums.CashOrderStatus;
import com.fivefy.domain.order.enums.CashProductType;
import com.fivefy.domain.order.repository.CashOrderRepository;
import com.fivefy.domain.payment.entity.Payment;
import com.fivefy.domain.payment.repository.PaymentRepository;
import com.fivefy.domain.user.repository.UserRepository;
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
     * 포인트 구매 (PG 연동 전 — 더미값 사용)
     * @param userId
     * @param request
     * @return
     */
    @Transactional
    public CashOrderPurchaseResponse purchase(Long userId, CashOrderVerifyRequest request) {

        CashProductType productType = request.productType();
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8);

        // PENDING으로 저장 — webhookId는 아직 없음, 포트원이 결제 후 웹훅으로 줌
        CashOrder cashOrder = CashOrder.create(userId, productType, orderNumber);
        cashOrderRepository.save(cashOrder);

        // orderNumber만 반환 — 프론트가 이걸로 포트원 SDK 결제창 호출
        return new CashOrderPurchaseResponse(orderNumber);
    }

    /**
     * 포인트 환불
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

        // 취소 응답 확인 정상 아님->취소실패
        if (!"SUCCEEDED".equals(cancelResponse.status())) {
            throw new IllegalStateException("포트원 결제 취소 실패: " + cancelResponse.status());
        }

        // DB 상태 변경
        cashOrder.refund();                 // CashOrder.status: SUCCESS → REFUNDED
        payment.refund(request.reason());   // Payment.status: COMPLETED → REFUNDED
                                            // Payment.refundReason: null → request.reason()
                                            // Payment.refundedAt: null → LocalDateTime.now()

        // 지갑의 포인트 차감
        Wallet wallet = walletRepository.findByUserId(cashOrder.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("지갑 없음"));
        wallet.useBalance(cashOrder.getPointAmount());
        // 지갑 내역(PointHistory)
        pointHistoryRepository.save(PointHistory.create(
            wallet.getId(), PointType.PAID, PointHistoryType.REFUND,
                cashOrder.getPointAmount(), wallet.getBalance(),
            "포인트 환불 (" + request.reason() + ")"
        ));

        return CashOrderResponse.from(cashOrder);
    }

    /**
     * 포트원이 결제 완료 후 서버로 보내주는 웹훅을 처리
     * PortoneWebhookController에서 보냄
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
        // 웹훅 수신 즉시 로그
        System.out.println("=== 웹훅 수신 ===");
        System.out.println("webhookId: " + webhookId);
        System.out.println("rawBody: " + rawBody);
        System.out.println("=================");

        // 웹훅 body 파싱
        PortoneWebhookRequest request;
        try {
            request = objectMapper.readValue(rawBody, PortoneWebhookRequest.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("웹훅 body 파싱 실패");
        }

        System.out.println("웹훅 타입: " + request.type());

        // 결제(예약 결제 포함)가 승인된 경우
        if (!"Transaction.Paid".equals(request.type())) {
            return;
        }
        // 시그니처 + 타임스탬프 검증 (위변조 / 리플레이 방지)
        portoneWebhookVerifier.verify(webhookId, webhookTimestamp, rawBody, webhookSignature);

        // webhook-id 중복 체크 (멱등성 보장)
        if (cashOrderRepository.existsByWebhookId(webhookId)) {
            return;
        }

        // 포트원 단건 조회
        String pgPaymentId = request.data().paymentId();  // ORD-xxxxxx
        PortonePaymentResponse pgPayment = portoneClient.getPayment(pgPaymentId);
        // CashOrder 조회
        CashOrder cashOrder = cashOrderRepository.findByOrderNumber(pgPayment.orderNumber())
                .orElseThrow(() -> new IllegalArgumentException("주문 없음"));


        // 상태(결제 대기)
        if (cashOrder.getStatus() != CashOrderStatus.PENDING) {
            return;
        }
        // 금액 검증
        // PortonePaymentResponse에서 결제 대금을 totalAmount으로 두고, 포인트 구매에서 결제 대금을 CashAmount로 둔다.
        if (!pgPayment.totalAmount().equals(cashOrder.getCashAmount())) {
            throw new IllegalStateException("결제 금액 불일치");
        }

        // CashOrder SUCCESS + webhookId 저장
        cashOrder.success(webhookId);

        // ⑨ Payment 기록
        Payment payment = Payment.create(
                cashOrder.getUserId(),
                cashOrder.getCashAmount(),
                cashOrder.getOrderNumber(),
                pgPaymentId,   // pgTransactionId — 포트원 결제 ID
                webhookId      // webhookId (=idempotencyKey, 멱등키) — 포트원이 준 웹훅 고유 ID
        );
        payment.complete();
        paymentRepository.save(payment);

        // Wallet 충전
        Wallet wallet = walletRepository.findByUserId(cashOrder.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("지갑 없음"));
        wallet.chargeBalance(cashOrder.getPointAmount());
        // 지갑 이력(PointHistory)
        pointHistoryRepository.save(PointHistory.create(
                wallet.getId(),
                PointType.PAID,
                PointHistoryType.CHARGE,
                cashOrder.getPointAmount(),
                wallet.getBalance(),
            "포인트 충전 (" + cashOrder.getProductType().getDescription() + ")"
        ));


    }
}