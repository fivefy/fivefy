package com.fivefy.domain.billingkey.repository;

import com.fivefy.domain.billingkey.entity.BillingKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingKeyRepository extends JpaRepository<BillingKey, Long> {

    /** 사용자의 활성 빌링키 단건 조회 (자동 청구용) */
    Optional<BillingKey> findByUserIdAndActiveTrue(Long userId);

    /** 스케줄러용 — 활성 빌링키 전체 조회 */
    List<BillingKey> findAllByActiveTrue();

    /** 빌링키 토큰 중복 등록 방지 */
    boolean existsByBillingKey(String billingKey);
}