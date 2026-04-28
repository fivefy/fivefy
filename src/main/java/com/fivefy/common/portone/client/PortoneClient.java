package com.fivefy.common.portone.client;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.common.portone.config.PortoneProperties;
import com.fivefy.common.portone.dto.PortoneBillingKeyResponse;
import com.fivefy.common.portone.dto.PortoneBillingPaymentResponse;
import com.fivefy.common.portone.dto.PortoneCancelResponse;
import com.fivefy.common.portone.dto.PortonePaymentResponse;
import com.fivefy.common.portone.enums.PortoneErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortoneClient {

    private final PortoneProperties portoneProperties;
    private final RestTemplate restTemplate;

    private static final String BASE_URL = "https://api.portone.io";

    /**
     * 포트원 결제(실제돈 → 포인트) 단건 조회
     * 웹훅에서 받은 paymentId(= orderNumber)로 포트원에 실제 결제 정보를 조회
     * 금액 검증, orderNumber 확인에 사용
     *
     * + storeId 누락 시 포트원이 어느 상점인지 모르므로 에러(404) 반환 (필수)
     * -> String url = BASE_URL + "/payments/" + paymentId + "?storeId=" + portoneProperties.storeId();
     * ++ 2026-04-22 : 하위 상점은  storeId가 필요하지만, 대표 상점은 필요 없다. 그래서 storeId 삭제
     * -> String url = BASE_URL + "/payments/" + paymentId;
     */
    public PortonePaymentResponse getPayment(String paymentId) {
        String url = BASE_URL + "/payments/" + paymentId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "PortOne " + portoneProperties.apiSecret());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // 실제 JSON 응답 확인용 — 포트원 응답 필드명 파악 후 PortonePaymentResponse에 반영
        ResponseEntity<PortonePaymentResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, PortonePaymentResponse.class
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new BusinessException(PortoneErrorCode.ERR_PORTONE_PAYMENT_NOT_FOUND);
        }

        return response.getBody();
    }

    /**
     * 결제 취소 (환불)
     * pgTransactionId(포트원 결제 ID)로 포트원에 취소 요청
     * storeId URL 파라미터 + body 양쪽에 포함 (포트원 V2 스펙)
     *                      → body는 취소사유, 취소금액, 식별
     * 전액 환불: amount = cashOrder.getCashAmount()
     */
    public PortoneCancelResponse cancelPayment(String paymentId, Long amount, String reason) {
        String url = BASE_URL + "/payments/"
                + paymentId + "/cancel"
                + "?storeId=" + portoneProperties.storeId();  // storeId 추가

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "PortOne " + portoneProperties.apiSecret());
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 포트원 취소 요청 body(양식 맞추기): reason(취소사유), amount(취소금액), 어느상점결제(식별)
        String body = """
                {
                    "reason": "%s",
                    "amount": %d,
                    "storeId": "%s"
                }
                """
                .formatted(reason, amount, portoneProperties.storeId());

        ResponseEntity<PortoneCancelResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), PortoneCancelResponse.class
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new BusinessException(PortoneErrorCode.ERR_PORTONE_CANCEL_FAILED);
        }

        return response.getBody();
    }


    /**
     * 빌링키 단건 조회
     * 프론트가 포트원 SDK로 카드 등록 완료 후 billingKeyId를 서버에 전달
     * 서버는 이 메서드로 실제 빌링키 토큰 + 카드 정보를 확인
     *
     * @param billingKeyId 포트원이 발급한 빌링키 ID (프론트에서 전달)
     */
     public PortoneBillingKeyResponse getBillingKey(String billingKeyId) {
         String url = BASE_URL + "/billing-keys/" + billingKeyId;

         ResponseEntity<PortoneBillingKeyResponse> response = restTemplate.exchange(
                 url, HttpMethod.GET, new HttpEntity<>(authHeaders()), PortoneBillingKeyResponse.class
         );

         if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
             throw new BusinessException(PortoneErrorCode.ERR_PORTONE_BILLING_KEY_NOT_FOUND);
         }
         return response.getBody();
     }

    /**
     * 빌링키로 카드 자동 청구
     * 스케줄러에서 매월 호출 — 사용자 개입 없이 카드에서 돈을 빼감
     *
     * @param billingKey  DB에 저장된 빌링키 토큰
     * @param orderNumber 이번 청구의 주문번호 (REC-xxxxxxxx)
     * @param amount      청구 금액 (원)
     * @param description 청구 설명 (카드 명세서에 표시)
     */
    public PortoneBillingPaymentResponse chargeWithBillingKey(
            String billingKey,
            String orderNumber,
            Long amount,
            String description
    ) {
        String url = BASE_URL + "/payments/" + orderNumber + "/billing-key";

        String body = """
                   {
                       "billingKey": "%s",
                       "orderName": "%s",
                       "amount": {
                           "total": %d
                       },
                       "currency": "KRW"
                   }
                   """
                   .formatted(billingKey, description, amount);

        ResponseEntity<PortoneBillingPaymentResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, authHeaders()), PortoneBillingPaymentResponse.class
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new BusinessException(PortoneErrorCode.ERR_PORTONE_BILLING_CHARGE_FAILED);
        }
        return response.getBody();
    }

    /**
     * 포트원 빌링키 삭제 (카드 해지 시 포트원 측 데이터도 정리)
     *
     * @param billingKeyId 포트원이 발급한 빌링키 ID
     */
    public void deleteBillingKey(String billingKeyId) {
        String url = BASE_URL + "/billing-keys/" + billingKeyId;

        ResponseEntity<Void> response = restTemplate.exchange(
                url, HttpMethod.DELETE, new HttpEntity<>(authHeaders()), Void.class
        );

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new BusinessException(PortoneErrorCode.ERR_PORTONE_BILLING_KEY_DELETE_FAILED);
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "PortOne " + portoneProperties.apiSecret());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}