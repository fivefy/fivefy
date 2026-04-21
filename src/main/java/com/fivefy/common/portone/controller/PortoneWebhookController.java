package com.fivefy.common.portone.controller;

import com.fivefy.domain.cashorder.service.CashOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhook/portone")
@RequiredArgsConstructor
public class PortoneWebhookController {

    private final CashOrderService cashOrderService;

    /**
     * 포트원 웹훅 수신
     * POST /api/webhook/portone
     *
     * @RequestBody를 String으로 받는 게 핵심
     *   → Jackson이 파싱하면 원본 바이트가 달라져 HMAC-SHA256 시그니처 검증 실패
     *   → rawBody 원본을 그대로 PortoneWebhookVerifier에 넘겨야 함
     *
     * 헤더 3종:
     * - webhook-id        : 웹훅 고유 ID (멱등키 — 중복 수신 방지)
     * - webhook-signature : HMAC-SHA256 서명 "v1,{base64}" 형식 (위변조 방지)
     * - webhook-timestamp : 전송 시각 Unix초 (리플레이 공격 방지)
     * @param webhookId
     * @param webhookSignature
     * @param webhookTimestamp
     * @param rawBody
     * @return
     */
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