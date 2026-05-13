package com.fivefy.domain.pointorder.controller;

import com.fivefy.domain.pointorder.dto.PointOrderPurchaseRequest;
import com.fivefy.domain.pointorder.service.PointOrderService;
import com.fivefy.domain.subscription.dto.SubscriptionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/point-orders")
@RequiredArgsConstructor
public class PointOrderController {

    private final PointOrderService pointOrderService;

    /**
     * 구독 구매
     * POST /api/me/point-orders/purchases
     */
    @PostMapping("/purchases")
    public ResponseEntity<SubscriptionResponse> purchase(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PointOrderPurchaseRequest request
    ) {
        //                      기존 : .ok() : 200 반환
        //                      현재 : 201 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(pointOrderService.purchase(userId, request));
    }
}
