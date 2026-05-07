package com.fivefy.common.portone;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.common.portone.config.PortoneProperties;
import com.fivefy.common.portone.enums.PortoneErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class PortoneWebhookVerifier {

    private final PortoneProperties portoneProperties;
    private static final long MAX_TIMESTAMP_DIFF_SECONDS = 300; // 5분

    /**
     * 웹훅 2단계 보안 검증(
     * (3단계 중 webhookId 중복 체크는 CashOrderService.processWebhook()에서 처리)
     *
     * 검증 순서:
     * 1. 타임스탬프 검증 — 현재 시각과 ±5분 초과 시 리플레이 공격으로 간주, 예외
     * 2. 시그니처 검증 — HMAC-SHA256 재계산 후 포트원이 보낸 값과 비교     * @param webhookId
     * @param webhookTimestamp
     * @param rawBody
     * @param webhookSignature
     */
    public void verify(String webhookId, String webhookTimestamp,
                       String rawBody, String webhookSignature) {

        // 타임스탬프 검증 (리플레이 공격 방지)
        long now = Instant.now().getEpochSecond();
        long sentAt;
        try {
            // 1단계 : 웹훅 타임스탬프가 아니면 즉시 차단 : 숫자가 아닌 문자나 날짜, 빈값 등
            sentAt = Long.parseLong(webhookTimestamp);
        } catch (NumberFormatException e) {
            throw new BusinessException(PortoneErrorCode.ERR_PORTONE_WEBHOOK_INVALID_TIMESTAMP);
        }
        // 2단계 : 숫자인데 현재 시각과 차이가 있음 = 리플레이 공격 차단
        if (Math.abs(now - sentAt) > MAX_TIMESTAMP_DIFF_SECONDS) {
            throw new BusinessException(PortoneErrorCode.ERR_PORTONE_WEBHOOK_TIMESTAMP_EXPIRED);
        }

        // 시그니처 검증 (위변조 방지)
        // 포트원 서명 형식:
        // 서명 대상 = "{webhookId}.{webhookTimestamp}.{rawBody}"
        // 서명 = HMAC-SHA256(서명 대상, 웹훅시크릿)
        String signedContent = webhookId + "." + webhookTimestamp + "." + rawBody;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            // 포트원 웹훅 시크릿은 "whsec_" 접두사 + Base64 인코딩된 값으로 구성
            // 주의! "whsec_" 접두사 제거 후 Base64 디코딩해서 실제 키 바이트 추출
            String secret = portoneProperties.webhookSecret();
            if (secret.startsWith("whsec_")) {
                secret = secret.substring("whsec_".length());
            }
            byte[] keyBytes = Base64.getDecoder().decode(secret); // ← 실제 키 바이트

            SecretKeySpec key = new SecretKeySpec(keyBytes, "HmacSHA256"); // ← keyBytes 사용
            mac.init(key);

            // 서명 재계산 후 Base64 인코딩
            String computed = Base64.getEncoder().encodeToString(
                mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8))
            );

            // 포트원 시그니처 : "v1,{base64}" 형식으로 전달 — 쉼표 뒤 base64 부분만 비교
            String expected = webhookSignature.contains(",")
                ? webhookSignature.split(",")[1]
                : webhookSignature;

            // 바이트 비교
            if (!MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),  // 서버가 계산한 서명
                expected.getBytes(StandardCharsets.UTF_8)   // 포트원이 보낸 서명
            )) {
                throw new BusinessException(PortoneErrorCode.ERR_PORTONE_WEBHOOK_SIGNATURE_MISMATCH);
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("시그니처 검증 중 오류", e);
        }
    }
}