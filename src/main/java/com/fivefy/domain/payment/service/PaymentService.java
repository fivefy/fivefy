package com.fivefy.domain.payment.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.cashorder.entity.CashOrder;
import com.fivefy.domain.cashorder.repository.CashOrderRepository;
import com.fivefy.domain.payment.dto.PaymentResponse;
import com.fivefy.domain.payment.entity.Payment;
import com.fivefy.domain.payment.enums.PaymentErrorCode;
import com.fivefy.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CashOrderRepository cashOrderRepository;  // userId → cashOrderId 변환용

    /**
     * 내 결제 내역 전체 조회
     * @param userId    : userId 기준으로 전체 Payment 반환
     * @return
     */
    public List<PaymentResponse> getMyPayments(Long userId) {
        // 1. userId로 해당 유저의 CashOrder id 목록 조회
        List<Long> cashOrderIds = cashOrderRepository.findAllByUserId(userId)
                .stream()
                .map(CashOrder::getId)
                .toList();

        // 2. cashOrderId 목록으로 Payment 조회
        return paymentRepository.findAllByCashOrderIdIn(cashOrderIds)
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

    /**
     * 결제 단건 조회
     * paymentId로 조회 후 CashOrder를 통해 소유자 검증
     * @param userId
     * @param paymentId
     * @return
     */
    public PaymentResponse getPayment(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.ERR_PAYMENT_NOT_FOUND));

        // CashOrder를 통해 소유자 검증
        CashOrder cashOrder = cashOrderRepository.findById(payment.getCashOrderId())
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.ERR_PAYMENT_NOT_FOUND));

        if (!cashOrder.getUserId().equals(userId)) {
            throw new BusinessException(PaymentErrorCode.ERR_PAYMENT_FORBIDDEN);
        }

        return PaymentResponse.from(payment);
    }
}