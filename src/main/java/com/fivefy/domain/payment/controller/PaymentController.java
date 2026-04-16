package com.fivefy.domain.payment.controller;

import com.fivefy.domain.payment.dto.PaymentResponse;
import com.fivefy.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 내 결제 내역 조회
     */
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getMyPayments(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(paymentService.getMyPayments(userId));
    }

    /**
     * 단건 조회
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getPayment(userId, paymentId));
    }
}