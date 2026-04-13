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

    /**
     * 지갑(빈지갑 생성 후 충전)
     * @param userId
     *        balance       : 유료 재화
     *        eventBalance  : 무료 재화
     *        totalBalance  : 유료 + 무료
     * @return
     */
    public static Wallet create(Long userId) {
        validateNonNull(userId, "userId");

        Wallet wallet = new Wallet();
            wallet.userId = userId;
            wallet.balance = 0L;
            wallet.eventBalance = 0L;
            wallet.totalBalance = 0L;

        return wallet;
    }
}