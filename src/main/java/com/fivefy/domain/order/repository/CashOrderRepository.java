package com.fivefy.domain.order.repository;

import com.fivefy.domain.order.entity.CashOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CashOrderRepository extends JpaRepository<CashOrder, Long> {
    Optional<CashOrder> findByOrderNumber(String orderNumber);
}