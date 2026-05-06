package com.fivefy.domain.billingkey.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.billingkey.entity.BillingKey;
import com.fivefy.domain.billingkey.enums.BillingKeyErrorCode;
import com.fivefy.domain.billingkey.repository.BillingKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BillingKey 관련 DB 작업 전담 서비스
 *
 * BillingKeyService에서 외부 API 호출(트랜잭션 밖),
 * 이 클래스에서 DB 저장만(@Transactional) 담당하도록 분리.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingKeyPersistenceService {

    private final BillingKeyRepository billingKeyRepository;

    /**
     * 빌링키 등록은 DB 저장만 담당
     * BillingKeyService.register()에서 portoneClient.getBillingKey() 성공 확인 후 호출
     */
    @Transactional
    public BillingKey saveBillingKey(
            Long userId,
            String billingKeyToken,
            String cardLast4,
            String payMethod,
            String cardName
    ) {
        BillingKey billingKey = BillingKey.create(userId, billingKeyToken, cardLast4, payMethod, cardName);
        billingKeyRepository.save(billingKey);

        log.info("[BillingKey] DB 저장 완료 userId={}, cardName={}, cardLast4={}", userId, cardName, cardLast4);
        return billingKey;
    }

    /**
     * 빌링키 해지는 DB 비활성화만 담당
     * BillingKeyService.deactivate()에서 portoneClient.deleteBillingKey() 성공 확인 후 호출
     */
    @Transactional
    public void deactivateBillingKey(Long billingKeyId) {  // BillingKey → Long으로 변경
        BillingKey billingKey = billingKeyRepository.findById(billingKeyId)
                .orElseThrow(() -> new BusinessException(BillingKeyErrorCode.ERR_BILLING_KEY_NOT_FOUND));
        billingKey.deactivate();  // 트랜잭션 안에서 조회했으므로 dirty checking 정상 작동
        log.info("[BillingKey] DB 비활성화 완료 userId={}", billingKey.getUserId());
    }
}