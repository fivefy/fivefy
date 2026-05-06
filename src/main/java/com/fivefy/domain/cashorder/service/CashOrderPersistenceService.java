package com.fivefy.domain.cashorder.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.billingkey.enums.BillingKeyErrorCode;
import com.fivefy.domain.billingkey.repository.BillingKeyRepository;
import com.fivefy.domain.cashorder.dto.CashOrderResponse;
import com.fivefy.domain.cashorder.entity.CashOrder;
import com.fivefy.domain.cashorder.enums.CashProductType;
import com.fivefy.domain.cashorder.repository.CashOrderRepository;
import com.fivefy.domain.billingkey.entity.BillingKey;
import com.fivefy.domain.payment.entity.Payment;
import com.fivefy.domain.payment.repository.PaymentRepository;
import com.fivefy.domain.wallet.entity.PointHistory;
import com.fivefy.domain.wallet.entity.Wallet;
import com.fivefy.domain.wallet.enums.PointHistoryType;
import com.fivefy.domain.wallet.enums.PointType;
import com.fivefy.domain.wallet.enums.WalletErrorCode;
import com.fivefy.domain.wallet.repository.PointHistoryRepository;
import com.fivefy.domain.wallet.repository.WalletRepository;
import com.fivefy.common.portone.dto.PortoneBillingPaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CashOrder 관련 DB 작업 전담 서비스
 *
 * CashOrderService에서 외부 API 호출(트랜잭션 밖),
 *       이 클래스에서 DB 저장만(@Transactional) 담당하도록 분리.
 *       + CashOrderService에서만 작성하면 너무 길어져서 무게감 감소
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashOrderPersistenceService {

    private final CashOrderRepository cashOrderRepository;
    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final BillingKeyRepository billingKeyRepository;

    /**
     * 환불은 DB 상태 변경만 담당
     *
     * CashOrderService.refund()에서 portoneClient.cancelPayment() 성공 확인 후 호출
     * 포트원 취소가 확정된 상태에서만 진입하므로 DB 작업만 트랜잭션으로 처리
     */
    @Transactional
    public CashOrderResponse saveRefundResult(CashOrder cashOrder, Payment payment, String reason) {

        // CashOrder 상태 변경  : SUCCESS → REFUNDED
        cashOrder.refund();
        // Payment 상태 변경    : COMPLETED → REFUNDED, 환불 사유/시간 기록
        payment.refund(reason);

        // 토끼 : cashOrder와 payment는 트랜잭션 밖에서 로드된 detached 엔티티라 CashOrder.refund() 호출 시 dirty checking이 작동하지 않아 DB에 반영이 안 될 수 있다.
        // 토끼 해결 : 명시적 save() 추가 — detached 엔티티여도 merge되어 DB 반영 보장
        cashOrderRepository.save(cashOrder);
        paymentRepository.save(payment);


        // 지갑 포인트 차감
        // 포인트를 이미 소진한 경우 잔액 부족 예외 발생 가능
        // → 스케줄러 도입 시 음수 잔액 허용 또는 별도 정책 필요
        Wallet wallet = walletRepository.findByUserId(cashOrder.getUserId())
                .orElseThrow(() -> new BusinessException(WalletErrorCode.ERR_WALLET_NOT_FOUND));
        wallet.refundBalance(cashOrder.getPointAmount());

        // 포인트 이력 기록
        pointHistoryRepository.save(PointHistory.create(
                wallet.getId(),
                PointType.PAID,
                PointHistoryType.REFUND,
                cashOrder.getPointAmount(),
                wallet.getBalance(),
                "포인트 환불 (" + reason + ")",
                cashOrder.getId(),
                null
        ));

        log.info("[CashOrder] 환불 DB 저장 완료 orderNumber={}", cashOrder.getOrderNumber());
        return CashOrderResponse.from(cashOrder);
    }

    /**
     * 정기충전 DB 저장
     * CashOrderService.processRecurringCharge()에서 portoneClient.chargeWithBillingKey() 성공 확인 후 호출
     * 포트원 청구가 확정된 상태에서만 진입하므로 DB 작업만 트랜잭션으로 처리
     */
    @Transactional
    public void saveRecurringChargeResult(
            BillingKey billingKey,    // Long userId → BillingKey billingKey 로 변경
            String orderNumber,
            CashProductType productType,
            PortoneBillingPaymentResponse pgResponse
    ) {
        Long userId = billingKey.getUserId();  // 내부에서 꺼내서 사용

        // CashOrder 생성 (SUCCESS) — 빌링키 청구는 webhookId 대신 orderNumber를 멱등키로 대체
        CashOrder cashOrder = CashOrder.create(userId, productType, orderNumber);
        cashOrder.success(orderNumber);
        cashOrderRepository.save(cashOrder);

        // Payment 기록 : 2026-04-22 : 실제 PG 거래 ID pgIxId 수정
        Payment payment = Payment.create(
                userId,
                productType.getCashAmount(),
                orderNumber,
                pgResponse.payment().pgTxId(),      // 포트원 결제 ID paymentId() -> 실제 PG 거래 ID는 pgTxId. PortoneBillingPaymentResponse 참고하기
                orderNumber                         // 멱등키 (orderNumber 재사용)
        );
        payment.complete();
        paymentRepository.save(payment);

        // 지갑 포인트 충전
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(WalletErrorCode.ERR_WALLET_NOT_FOUND));
        wallet.chargeBalance(productType.getPointAmount());

        // 포인트 이력 기록
        pointHistoryRepository.save(PointHistory.create(
                wallet.getId(),
                PointType.PAID,
                PointHistoryType.CHARGE,
                productType.getPointAmount(),
                wallet.getBalance(),
                "정기 포인트 자동 충전",
                cashOrder.getId(),
                null
        ));

        log.info("[정기충전] DB 저장 완료 userId={}, orderNumber={}", userId, orderNumber);
    }
}