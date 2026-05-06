package com.fivefy.domain.billingkey.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.billingkey.enums.BillingKeyErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

@Entity
@Getter
@Table(name = "billing_keys")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BillingKey extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /**
     * 포트원이 발급한 빌링키 토큰 : 이걸 아임포트에서 -> 그게 포트원이래
     * 실제 카드 번호 대신 이 값으로 반복 청구
     */
    @Column(nullable = false, unique = true)
    private String billingKey;

    /**
     * 카드 끝 4자리 (화면 표시용)
     */
    @Column(length = 10)
    private String cardLast4;

    /**
     * 카드사 이름 (화면 표시용, ex: "현대카드")
     */
    @Column(length = 50)
    private String cardName;

    /**
     * 간편결제 - 카카오페이
     */
    @Column(length = 50)
    private String payMethod;

    /**
     * 활성 여부 — false면 자동 청구 스킵
     */
    @Column(nullable = false)
    private boolean active = true;

    /**
     * 빌링키 생성
     *
     * @param userId
     * @param billingKey
     * @param cardLast4
     * @param payMethod
     * @param cardName
     * @return
     */
    public static BillingKey create(Long userId, String billingKey,
                                    String cardLast4, String payMethod, String cardName) {
        validateNonNull(userId, "userId");
        validateNonNull(billingKey, "billingKey");

        BillingKey billingkey = new BillingKey();
        billingkey.userId     = userId;
        billingkey.billingKey = billingKey;
        billingkey.cardLast4  = cardLast4;
        billingkey.payMethod  = payMethod;
        billingkey.cardName   = cardName;
        billingkey.active     = true;

        return billingkey;
    }

    /**
     * 빌링키 해지(카드 삭제)
     */
    public void deactivate() {
        if (!this.active) {
            throw new BusinessException(BillingKeyErrorCode.ERR_BILLING_KEY_ALREADY_DEACTIVATED);
        }

        this.active = false;
    }
}