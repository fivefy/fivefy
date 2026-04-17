package com.fivefy.domain.payment.service;

import com.fivefy.domain.payment.dto.PaymentResponse;
import com.fivefy.domain.payment.entity.Payment;
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

    /**
     * 내 결제 내역 전체 조회
     * @param userId    : userId 기준으로 전체 Payment 반환
     * @return
     */
    public List<PaymentResponse> getMyPayments(Long userId) {
        return paymentRepository.findAllByUserId(userId).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    /**
     * 결제 단건 조회
     * paymentId로 조회 후 소유자 검증
     * @param userId
     * @param paymentId
     * @return
     */
    public PaymentResponse getPayment(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 내역 없음"));

        if (!payment.getUserId().equals(userId)) {
            throw new IllegalStateException("본인 결제만 조회 가능");
        }

        return PaymentResponse.from(payment);
    }
}