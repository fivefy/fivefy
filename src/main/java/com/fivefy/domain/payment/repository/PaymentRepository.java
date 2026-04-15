package com.fivefy.domain.payment.repository;

import com.fivefy.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 멱등키 확인(포트원)
    boolean existsByIdempotencyKey(String idempotencyKey);

    // webhook, 중복 방지
    boolean existsByPgTransactionId(String pgTransactionId);

    // 유저 결제기록
    List<Payment> findAllByUserId(Long userId);

    // 환불 시 Payment 조회용
    Optional<Payment> findByPgTransactionId(String pgTransactionId);

    // 주문 기록(포인트 패키지 상품 번호 : 실제 돈 -> 포인트)
    Optional<Payment> findByOrderNumber(String orderNumber);
}