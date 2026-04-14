package com.fivefy.domain.wallet.controller;

import com.fivefy.domain.wallet.dto.WalletResponse;
import com.fivefy.domain.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    /**
     * 내 지갑 조회
     * GET /api/me/wallets
     */
    @GetMapping
    public ResponseEntity<WalletResponse> getMyWallet(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(walletService.getMyWallet(userId));
    }
}