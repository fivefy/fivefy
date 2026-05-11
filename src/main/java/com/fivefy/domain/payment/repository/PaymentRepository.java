package com.fivefy.domain.payment.repository;

import com.fivefy.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // webhook, 중복 방지
    boolean existsByPgTransactionId(String pgTransactionId);

    // 유저 결제기록 : UserId -> CashOrder을 통해서 추출
    // List<Payment> findAllByUserId(Long userId);
    List<Payment> findAllByCashOrderIdIn(List<Long> cashOrderIds);

    // 환불 시 Payment 조회용
    Optional<Payment> findByPgTransactionId(String pgTransactionId);

    // 주문 기록(포인트 패키지 상품 번호 : 실제 돈 -> 포인트)
    Optional<Payment> findByOrderNumber(String orderNumber);
}