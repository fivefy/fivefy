package com.fivefy.domain.cashorder.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.cashorder.enums.CashOrderErrorCode;
import com.fivefy.domain.cashorder.enums.CashOrderStatus;
import com.fivefy.domain.cashorder.enums.CashProductType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

@Entity
@Getter
@Table(name = "cash_orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CashOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /**
     * 구독 패키지 상품 번호(상점이 없으니 자체적으로 진행)
     * 1번 : 1,000원 = 1,000포인트
     * 2번 : 2,000원 = 2,500포인트
     * 3번 : 0원 = 150 포인트(계정당 1회 이용 가능)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CashProductType productType;

    @Column(nullable = false)
    private Long cashAmount;    // 결제하는 실제 돈

    @Column(nullable = false)
    private Long pointAmount;   // 교환하는 포인트

    @Column(length = 50, nullable = false, unique = true)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CashOrderStatus status;

    @Column(unique = true)
    private String webhookId;

    public static CashOrder create(Long userId, CashProductType productType, String orderNumber) {
        validateNonNull(userId, "userId");
        validateNonNull(productType, "productType");
        validateNonNull(orderNumber, "orderNumber");

        CashOrder cashOrder = new CashOrder();
            cashOrder.userId = userId;
            cashOrder.productType = productType;                    // 상품 타입
            cashOrder.cashAmount = productType.getCashAmount();     // 상품 타입 가격(원)
            cashOrder.pointAmount = productType.getPointAmount();   // 상품(포인트)
            cashOrder.orderNumber = orderNumber;
            cashOrder.status = CashOrderStatus.PENDING;             // 상태 : 결제 대기

        return cashOrder;
    }

    /**
     * 결제 완료 처리 (PENDING → SUCCESS)
     * processWebhook()에서 금액 검증 통과 후 호출
     * @param webhookId : 웹훅 ID 저장 : 중복 웹훅 수신 시 existsByWebhookId()로 차단
     */
    public void success(String webhookId) {
        if (this.status != CashOrderStatus.PENDING) {
            throw new BusinessException(CashOrderErrorCode.ERR_CASH_ORDER_INVALID_STATUS_SUCCESS);
        }
        this.status = CashOrderStatus.SUCCESS;
        this.webhookId = webhookId;
    }

    /**
     *  환불 처리 (SUCCESS → REFUNDE
     *  refund()에서 포트원 취소 API 성공 확인 후 호출
     */
    public void refund() {
        if (this.status != CashOrderStatus.SUCCESS) {
            throw new BusinessException(CashOrderErrorCode.ERR_CASH_ORDER_INVALID_STATUS_REFUND);
        }
        this.status = CashOrderStatus.REFUNDED;
    }
}