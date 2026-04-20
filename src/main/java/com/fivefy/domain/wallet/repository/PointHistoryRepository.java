package com.fivefy.domain.wallet.repository;

import com.fivefy.domain.wallet.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    List<PointHistory> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    // 테스트 정리 코드
    void deleteAllByWalletId(Long walletId);
}