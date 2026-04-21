package com.fivefy.domain.wallet.dto;

import com.fivefy.domain.wallet.entity.Wallet;

public record WalletResponse(
        Long walletId,
        Long balance,
        Long eventBalance,
        Long totalBalance
) {
    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getBalance(),
                wallet.getEventBalance(),
                wallet.getTotalBalance()
        );
    }
}
