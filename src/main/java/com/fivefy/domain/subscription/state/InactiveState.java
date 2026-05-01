package com.fivefy.domain.subscription.state;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.subscription.entity.Subscription;
import com.fivefy.domain.subscription.enums.*;

public class InactiveState implements SubscriptionState {

    // 클래스 로드 시 객체 생성
    public static final InactiveState INSTANCE = new InactiveState();

    // 외부에서 new 못하게 막음
    private InactiveState() {}

    /**
     * 비활성화 → 취소
     * - 아직 활성화 안된 구독 취소 가능
     */
    @Override
    public void cancel(Subscription subscription) {
        subscription.changeStatus(SubscriptionStatus.CANCELED);
        subscription.clearNextBillingDate();
    }

    /**
     * 비활성화 → 만료
     */
    @Override
    public void expire(Subscription subscription) {
        subscription.changeStatus(SubscriptionStatus.EXPIRE);
    }

    /**
     * 비활성화 상태에서는 갱신 불가
     */
    @Override
    public void renew(Subscription subscription) {
        throw new BusinessException(SubscriptionErrorCode.ERR_SUBSCRIPTION_ALREADY_CANCELED);
    }
}