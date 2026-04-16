package com.fivefy.common.portone.client;

import com.fivefy.common.portone.config.PortoneProperties;
import com.fivefy.common.portone.dto.PortoneCancelResponse;
import com.fivefy.common.portone.dto.PortonePaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class PortoneClient {

    private final PortoneProperties portoneProperties;
    private final RestTemplate restTemplate;

    private static final String BASE_URL = "https://api.portone.io";

    /**
     * 결제 단건 조회
     * 웹훅에서 받은 paymentId로 포트원에 실제 결제 정보를 조회
     * — 금액 검증, merchantUid(orderNumber) 확인에 사용
     */
    public PortonePaymentResponse getPayment(String paymentId) {
        // String url = BASE_URL + "/payments/" + paymentId;
        String url = BASE_URL + "/payments/" + paymentId + "?storeId=" + portoneProperties.storeId();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "PortOne " + portoneProperties.apiSecret());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // 1. 실제 JSON 응답 확인용 — 포트원 응답 필드명 파악 후 PortonePaymentResponse에 반영
        ResponseEntity<PortonePaymentResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, PortonePaymentResponse.class
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new IllegalStateException("포트원 결제 조회 실패");
        }

        return response.getBody();
    }

    /**
     * 결제 취소 (환불)
     * pgTransactionId(포트원 결제 ID)로 포트원에 취소 요청
     * 전액 환불: amount = cashOrder.getCashAmount()
     */
    public PortoneCancelResponse cancelPayment(String paymentId, Long amount, String reason) {
        String url = BASE_URL + "/payments/"
                + paymentId + "/cancel"
                + "?storeId=" + portoneProperties.storeId();  // storeId 추가

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "PortOne " + portoneProperties.apiSecret());
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 포트원 취소 요청 body: reason(취소사유), amount(취소금액), 어느상점결제(식별)
        String body = """
                {
                    "reason": "%s",
                    "amount": %d,
                    "storeId": "%s"
                }
                """.formatted(reason, amount, portoneProperties.storeId());

        ResponseEntity<PortoneCancelResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), PortoneCancelResponse.class
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new IllegalStateException("포트원 환불 실패");
        }

        return response.getBody();
    }
}