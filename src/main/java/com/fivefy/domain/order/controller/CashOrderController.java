package com.fivefy.domain.order.controller;

import com.fivefy.domain.order.dto.CashOrderRefundRequest;
import com.fivefy.domain.order.dto.CashOrderResponse;
import com.fivefy.domain.order.dto.CashOrderVerifyRequest;
import com.fivefy.domain.order.service.CashOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cash-orders")
@RequiredArgsConstructor
public class CashOrderController {

    private final CashOrderService cashOrderService;

    /**
     * 결제 검증 + 포인트 충전
     * @param userId
     * @param request
     * @return
     */
    @PostMapping("/purchase")
    public ResponseEntity<CashOrderResponse> purchase(
            @AuthenticationPrincipal Long userId,
            @RequestBody CashOrderVerifyRequest request
    ) {

        return ResponseEntity.ok(cashOrderService.purchase(userId, request));
    }

    /**
     * 환불
     * @param userId
     * @param request
     * @return
     */
    @PostMapping("/refund")
    public ResponseEntity<CashOrderResponse> refund(
            @AuthenticationPrincipal Long userId,
            @RequestBody CashOrderRefundRequest request
    ) {

        return ResponseEntity.ok(cashOrderService.refund(userId, request));
    }
}