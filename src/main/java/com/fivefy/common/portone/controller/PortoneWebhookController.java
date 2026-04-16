package com.fivefy.common.portone.controller;

import com.fivefy.common.portone.dto.PortoneWebhookRequest;
import com.fivefy.domain.order.service.CashOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhook/portone")
@RequiredArgsConstructor
public class PortoneWebhookController {

    private final CashOrderService cashOrderService;

    @PostMapping
    public ResponseEntity<Void> webhook(
            // 웹훅에서 제공하는 헤더 3가지 : 웹훅ID(멱등키, 중복 방지), 웹훅시그니처(시크릿키?), 웹훅타임스탬프(연속방지)
            @RequestHeader("webhook-id") String webhookId,
            @RequestHeader("webhook-signature") String webhookSignature,
            @RequestHeader("webhook-timestamp") String webhookTimestamp,
            @RequestBody String rawBody
    ) {  // String으로 받는 게 핵심 — 서명 검증에 원본 필요

        cashOrderService.processWebhook(
            webhookId, webhookSignature, webhookTimestamp, rawBody
        );
        return ResponseEntity.ok().build();
    }
}