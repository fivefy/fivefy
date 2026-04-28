package com.fivefy.domain.wallet.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.wallet.dto.PointHistoryResponse;
import com.fivefy.domain.wallet.dto.WalletResponse;
import com.fivefy.domain.wallet.entity.Wallet;
import com.fivefy.domain.wallet.enums.WalletErrorCode;
import com.fivefy.domain.wallet.repository.PointHistoryRepository;
import com.fivefy.domain.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 지갑 조회한 userID 자체는 중요하지 않기에 debug 설정
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WalletService {

    private final WalletRepository walletRepository;
    private final PointHistoryRepository pointHistoryRepository;

    // 지갑 조회
    @Transactional(readOnly = true)
    public WalletResponse getMyWallet(Long userId) {
        log.debug("[Wallet] 지갑 조회 userId={}", userId);
        return WalletResponse.from(getWallet(userId));  // getWallet() 재사용
    }

    // 지갑 여부 확인 시
    private Wallet getWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(WalletErrorCode.ERR_WALLET_NOT_FOUND));
    }

    // 지갑 내역 확인 시
    @Transactional(readOnly = true)
    public List<PointHistoryResponse> getHistories(Long userId) {
        log.debug("[Wallet] 포인트 내역 조회 userId={}", userId);
        // 지갑 여부 확인 시
        // 지갑 존재 여부 확인 후 walletId 기준으로 이력 조회
        // 최신순 정렬 (createdAt DESC)
        Wallet wallet = getWallet(userId);
        return pointHistoryRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId())
                .stream()
                .map(PointHistoryResponse::from)
                .toList();
    }
}