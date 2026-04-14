package com.fivefy.domain.payment.repository;

import com.fivefy.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 결제 시 검증
    boolean existsByIdempotencyKey(String idempotencyKey);
}