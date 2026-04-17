package com.fivefy.domain.wallet.service;

import com.fivefy.domain.wallet.dto.PointHistoryResponse;
import com.fivefy.domain.wallet.dto.WalletResponse;
import com.fivefy.domain.wallet.entity.Wallet;
import com.fivefy.domain.wallet.repository.PointHistoryRepository;
import com.fivefy.domain.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WalletService {

    private final WalletRepository walletRepository;
    private final PointHistoryRepository pointHistoryRepository;

    // 지갑 조회
    @Transactional(readOnly = true)
    public WalletResponse getMyWallet(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("지갑을 찾을 수 없습니다."));
        return WalletResponse.from(wallet);
    }

    // 지갑 여부 확인 시
    private Wallet getWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("지갑 없음"));
    }

    // 지갑 내역 확인 시
    @Transactional(readOnly = true)
    public List<PointHistoryResponse> getHistories(Long userId) {
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