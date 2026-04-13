package com.fivefy.domain.wallet.entity;

import com.fivefy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

@Entity
@Getter
@Table(name = "wallet")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long balance;

    @Column(nullable = false)
    private Long eventBalance;

    @Column(nullable = false)
    private Long totalBalance;

    public static Wallet create(Long userId) {
        validateNonNull(userId, "유저 ID");

        Wallet wallet = new Wallet();
            wallet.userId = userId;
            wallet.balance = 0L;
            wallet.eventBalance = 0L;
            wallet.totalBalance = 0L;

        return wallet;
    }
}