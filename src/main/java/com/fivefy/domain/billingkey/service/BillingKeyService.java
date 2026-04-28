package com.fivefy.domain.billingkey.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.common.portone.client.PortoneClient;
import com.fivefy.common.portone.dto.PortoneBillingKeyResponse;
import com.fivefy.domain.billingkey.dto.BillingKeyRegisterRequest;
import com.fivefy.domain.billingkey.dto.BillingKeyResponse;
import com.fivefy.domain.billingkey.entity.BillingKey;
import com.fivefy.domain.billingkey.enums.BillingKeyErrorCode;
import com.fivefy.domain.billingkey.repository.BillingKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingKeyService {

    private final BillingKeyRepository billingKeyRepository;
    private final PortoneClient portoneClient;

    /**
     * 빌링키 등록 방법
     *
     * 1. 프론트가 포트원 SDK로 카드 등록 → 포트원이 billingKeyId 발급 → 프론트가 서버에 전달
     *      - 이걸 어떻게 하냐고...
     * 2. 서버가 포트원에 billingKeyId로 단건 조회 → 빌링키 토큰 + 카드 정보 확인
     * 3. DB에 BillingKey 저장
     *
     * @param userId
     * @param request billingKeyId (포트원이 발급한 ID, 프론트에서 전달)
     */
    @Transactional
    public BillingKeyResponse register(Long userId, BillingKeyRegisterRequest request) {

        log.info("[BillingKey] 등록 요청 userId={}, billingKeyId={}", userId, request.billingKeyId());

        // 1. 포트원에서 빌링키 정보 조회
        PortoneBillingKeyResponse portoneResponse = portoneClient.getBillingKey(request.billingKeyId());

        String billingKeyToken = portoneResponse.billingKey();

        // 중복 등록 방지
        if (billingKeyRepository.existsByBillingKey(billingKeyToken)) {
            throw new BusinessException(BillingKeyErrorCode.ERR_BILLING_KEY_ALREADY_EXISTS);
        }

        // 카드 정보 파싱 (끝 4자리)
        String cardLast4 = null;
        String cardName  = null;
        // 간편결제
        String payMethod = null;

        if (portoneResponse.card() != null) {
            // 카드 결제
            cardName = portoneResponse.card().name();
            String number = portoneResponse.card().number();
            if (number != null && number.length() >= 4) {
                // 4자리만
                cardLast4 = number.substring(number.length() - 4);
            }
            // 난 카드 안씀 -> 카카오페이 추가
        } else if (portoneResponse.easyPay() != null) {
            // 간편결제 (카카오페이 등) — 카드번호 없음
            cardName  = portoneResponse.easyPay().provider(); // "KAKAOPAY"
            cardLast4 = null; // 카카오페이는 끝 4자리 없음
            payMethod = "KAKAOPAY"; // 기본값 : 카카오페이
        }

        // 2. DB 저장
        BillingKey billingKey = BillingKey.create(userId, billingKeyToken, cardLast4, payMethod, cardName);
        billingKeyRepository.save(billingKey);

        log.info("빌링키 등록 완료 — userId={}, cardName={}, cardLast4={}", userId, cardName, cardLast4);

        return BillingKeyResponse.from(billingKey);
    }

    /**
     * 빌링키 해지 (카드 삭제)
     *
     * 흐름:
     * 1. DB에서 빌링키 조회 + 본인 검증
     * 2. 포트원에 빌링키 삭제 요청
     * 3. DB에서 비활성화 (active = false)
     *
     * @param userId
     * @param billingKeyId DB PK
     */
    @Transactional
    public void deactivate(Long userId, Long billingKeyId) {

        log.info("[BillingKey] 해지 요청 userId={}, billingKeyId={}", userId, billingKeyId);

        BillingKey billingKey = billingKeyRepository.findById(billingKeyId)
                .orElseThrow(() -> new BusinessException(BillingKeyErrorCode.ERR_BILLING_KEY_NOT_FOUND));

        if (!billingKey.getUserId().equals(userId)) {
            throw new BusinessException(BillingKeyErrorCode.ERR_BILLING_KEY_FORBIDDEN);
        }

        // 포트원 측 빌링키 삭제
        portoneClient.deleteBillingKey(billingKey.getBillingKey());

        // DB 비활성화
        billingKey.deactivate();

        log.info("빌링키 해지 완료 — userId={}, billingKeyId={}", userId, billingKeyId);
    }
}