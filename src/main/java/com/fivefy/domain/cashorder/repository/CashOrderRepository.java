package com.fivefy.domain.cashorder.repository;

import com.fivefy.domain.cashorder.entity.CashOrder;
import com.fivefy.domain.cashorder.enums.CashOrderStatus;
import com.fivefy.domain.cashorder.enums.CashProductType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CashOrderRepository extends JpaRepository<CashOrder, Long> {

    // 웹훅 : 중복방지 확인용(멱등키)
    boolean existsByWebhookId(String webhookId);

    // 결제 : 주문 확인
    Optional<CashOrder> findByOrderNumber(String orderNumber);

    // PaymentService에서 유저의 cashOrderId 목록 조회용
    List<CashOrder> findAllByUserId(Long userId);

    // 포인트 충전 : 150P(PRODUCT_3) 중복 사용 방지용
    boolean existsByUserIdAndProductTypeAndStatus(Long userId, CashProductType productType, CashOrderStatus status);
}