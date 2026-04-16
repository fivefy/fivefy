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

    public List<PaymentResponse> getMyPayments(Long userId) {
        return paymentRepository.findAllByUserId(userId).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    public PaymentResponse getPayment(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 내역 없음"));

        if (!payment.getUserId().equals(userId)) {
            throw new IllegalStateException("본인 결제만 조회 가능");
        }

        return PaymentResponse.from(payment);
    }
}