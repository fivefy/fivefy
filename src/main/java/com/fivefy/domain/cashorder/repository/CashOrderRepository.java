package com.fivefy.domain.cashorder.repository;

import com.fivefy.domain.cashorder.entity.CashOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CashOrderRepository extends JpaRepository<CashOrder, Long> {

    // 웹훅 : 중복방지 확인용(멱등키)
    boolean existsByWebhookId(String webhookId);

    // 결제 : 주문 확인
    Optional<CashOrder> findByOrderNumber(String orderNumber);
}