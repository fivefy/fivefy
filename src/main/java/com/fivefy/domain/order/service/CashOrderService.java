package com.fivefy.domain.order.service;

import com.fivefy.domain.order.dto.CashOrderRefundRequest;
import com.fivefy.domain.order.dto.CashOrderResponse;
import com.fivefy.domain.order.dto.CashOrderVerifyRequest;
import com.fivefy.domain.order.entity.CashOrder;
import com.fivefy.domain.order.enums.CashProductType;
import com.fivefy.domain.order.repository.CashOrderRepository;
import com.fivefy.domain.payment.entity.Payment;
import com.fivefy.domain.payment.repository.PaymentRepository;
import com.fivefy.domain.wallet.entity.PointHistory;
import com.fivefy.domain.wallet.entity.Wallet;
import com.fivefy.domain.wallet.enums.PointHistoryType;
import com.fivefy.domain.wallet.enums.PointType;
import com.fivefy.domain.wallet.repository.PointHistoryRepository;
import com.fivefy.domain.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CashOrderService {

    private final CashOrderRepository cashOrderRepository;
    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    private final PointHistoryRepository pointHistoryRepository;

    /**
     * 포인트 구매 (PG 연동 전 — 더미값 사용)
     * @param userId
     * @param request
     * @return
     */
    @Transactional
    public CashOrderResponse purchase(Long userId, CashOrderVerifyRequest request) {

        CashProductType productType = request.productType();
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8);
        String dummyPgId = "PG-DUMMY-" + UUID.randomUUID().toString().substring(0, 8);
        String idempotencyKey = UUID.randomUUID().toString();

        // 1. CashOrder 생성
        CashOrder order = CashOrder.create(userId, productType, orderNumber);
        order.success();
        cashOrderRepository.save(order);

        // 2. Payment 기록 (더미 PG)
        Payment payment = Payment.create(
                userId,
                productType.getCashAmount(),
                orderNumber,
                dummyPgId,
                idempotencyKey
        );
        payment.complete();
        paymentRepository.save(payment);

        // 3. Wallet 충전
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("지갑 없음"));

        wallet.chargeBalance(productType.getPointAmount());

        // 4. PointHistory 기록
        pointHistoryRepository.save(
                PointHistory.create(
                        wallet.getId(),
                        PointType.PAID,
                        PointHistoryType.CHARGE,
                        productType.getPointAmount(),
                        wallet.getBalance(),
                        "포인트 충전 (" + productType.getDescription() + ")"
                )
        );

        return CashOrderResponse.from(order);
    }
    
    /**
     * 포인트 환불
     * @param userId
     * @param request
     * @return
     */
    @Transactional
    public CashOrderResponse refund(Long userId, CashOrderRefundRequest request) {

        // 1. CashOrder 조회
        CashOrder order = cashOrderRepository.findByOrderNumber(request.orderNumber())
                .orElseThrow(() -> new IllegalArgumentException("주문 없음"));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalStateException("본인 주문만 환불 가능");
        }

        // 2. CashOrder 상태 변경 : REFUND
        order.refund();

        // 3. Payment 상태 변경 (orderNumber로 조회 : 상태변경)
        Payment payment = paymentRepository.findByOrderNumber(order.getOrderNumber())
                .orElseThrow(() -> new IllegalArgumentException("결제 기록 없음"));
        payment.refund(request.reason());

        // 4. Wallet 차감
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("지갑 없음"));

        wallet.useBalance(order.getPointAmount());

        // 5. PointHistory 기록
        pointHistoryRepository.save(
                PointHistory.create(
                        wallet.getId(),
                        PointType.PAID,
                        PointHistoryType.REFUND,
                        order.getPointAmount(),
                        wallet.getBalance(),
                        "포인트 환불 (" + order.getProductType().getDescription() + ")"
                )
        );

        return CashOrderResponse.from(order);
    }
}