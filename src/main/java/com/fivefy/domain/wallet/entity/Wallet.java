package com.fivefy.domain.wallet.entity;

import com.fivefy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

@Entity
@Getter
@Table(name = "wallets")
@EntityListeners(AuditingEntityListener.class)  // @Last... 이거에 쓰임
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

    /**
     * 무료 포인트 충전 (이벤트·관리자 지급 시 호출)
     * eventBalance 증가 후 totalBalance 재계산
     * @param amount
     */
    public void chargeBalance(Long amount) {
        this.balance += amount;
        this.totalBalance = this.balance + this.eventBalance;
    }
    public void chargeEventBalance(Long amount) {
        this.eventBalance += amount;
        this.totalBalance = this.balance + this.eventBalance;
    }

    /**
     * 유료 포인트 사용 (PointOrder 구독 구매 시 호출)
     * 잔액 부족 시 예외 발생 (환불 시 호출하면 안 됨 → 별도 처리 필요)
     * @param amount
     */
    public void useBalance(Long amount) {
        if (this.balance < amount) {
            throw new IllegalArgumentException("유료 포인트 부족");
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
            throw new IllegalArgumentException("무료 포인트 부족");
        }
        this.eventBalance -= amount;
        this.totalBalance = this.balance + this.eventBalance;
    }
}