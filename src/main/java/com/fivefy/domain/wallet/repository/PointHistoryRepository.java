package com.fivefy.domain.wallet.repository;

import com.fivefy.domain.wallet.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
}