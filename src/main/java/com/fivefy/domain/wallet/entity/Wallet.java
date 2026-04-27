package com.fivefy.domain.wallet.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.wallet.enums.WalletErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

@Slf4j
@Entity
@Getter
@Table(name = "wallets")
@EntityListeners(AuditingEntityListener.class)  // @LastModifiedDate 동작을 위한 JPA 이벤트 리스너
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long balance;       // 유료

    @Column(nullable = false)
    private Long eventBalance;  // 무료

    @Column(nullable = false)
    private Long totalBalance;  // 통합

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 지갑 생성 팩토리 메서드
     * 회원가입 시 호출, 모든 잔액 0으로 초기화
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
            wallet.updatedAt = LocalDateTime.now();

        return wallet;
    }

    // ------------- 충전 ------------- //
    /**
     * 유료 포인트 충전 (CashOrder 결제 완료 웹훅 처리 시 호출)
     * balance 증가 후 totalBalance 재계산
     * @param amount
     */
    public void chargeBalance(Long amount) {
        this.balance += amount;
        this.totalBalance = this.balance + this.eventBalance;
    }

    /**
     * 무료 포인트 충전 (이벤트·관리자 지급 시 호출)
     * eventBalance 증가 후 totalBalance 재계산
     * @param amount
     */
    public void chargeEventBalance(Long amount) {
        this.eventBalance += amount;
        this.totalBalance = this.balance + this.eventBalance;
    }

    // ------------- 사용 ------------- //
    /**
     * 유료 포인트 사용 (PointOrder 구독 구매 시 호출)
     * 잔액 부족 시 예외 발생 (환불 시 호출하면 안 됨 → 별도 처리 필요)
     * @param amount
     */
    public void useBalance(Long amount) {
        if (this.balance < amount) {
            throw new BusinessException(WalletErrorCode.ERR_WALLET_PAID_BALANCE_INSUFFICIENT);
        }
        this.balance -= amount;
        this.totalBalance = this.balance + this.eventBalance;
    }

    /**
     * 무료 포인트 사용 (무료 포인트로 구독 구매 시 호출)
     * 잔액 부족 시 예외 발생(현재 기능하진 않음)
     * @param amount
     */
    public void useEventBalance(Long amount) {
        if (this.eventBalance < amount) {
            throw new BusinessException(WalletErrorCode.ERR_WALLET_FREE_BALANCE_INSUFFICIENT);
        }
        this.eventBalance -= amount;
        this.totalBalance = this.balance + this.eventBalance;
    }

    /**
     * 무료 → 유료 순서로 포인트 차감 (구독 구매 시 사용)
     * 무료 포인트를 먼저 소진하고, 부족한 만큼 유료 포인트에서 차감
     * 총 잔액(totalBalance) 기준으로 부족 여부 확인
     */
    public void useBalanceWithPriority(Long amount) {
        if (this.totalBalance < amount) {
            log.warn("포인트 부족 — 필요: {}P, 보유: {}P, 부족: {}P",
                    amount, this.totalBalance, amount - this.totalBalance);
            throw new BusinessException(WalletErrorCode.ERR_WALLET_TOTAL_BALANCE_INSUFFICIENT);
        }
        long fromFree = Math.min(this.eventBalance, amount);  // 무료에서 차감 가능한 양
        long fromPaid = amount - fromFree;                    // 나머지는 유료에서

        this.eventBalance -= fromFree;
        this.balance      -= fromPaid;
        this.totalBalance = this.balance + this.eventBalance;
    }

    // ── 환불 ────────────────────────────────────────────────

    /**
     * 환불 전용 유료 포인트 차감 — 음수 잔액 허용
     * CashOrder 환불 시 포인트를 이미 사용한 경우에도 처리 가능
     * 음수 상태는 다음 충전 시 상계됨
     */
    public void refundBalance(Long amount) {
        this.balance -= amount;  // 음수 허용
        this.totalBalance = this.balance + this.eventBalance;
    }
}