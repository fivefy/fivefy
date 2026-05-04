package com.fivefy.domain.subscription.state;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.subscription.entity.Subscription;
import com.fivefy.domain.subscription.enums.*;

public class ExpiredState implements SubscriptionState {

    // 클래스 로드 시 객체 생성
    public static final ExpiredState INSTANCE = new ExpiredState();

    // 외부에서 new 못하게 막음
    private ExpiredState() {}

    /**
     * 만료 상태에서 취소
     * 만료 상태에서는 어떤 행동도 불가
     * @param subscription
     */
    @Override
    public void cancel(Subscription subscription) {
        throw new BusinessException(SubscriptionErrorCode.ERR_SUBSCRIPTION_INVALID_STATUS_CANCEL);
    }

    /**
     * 만료 상태에서 만료
     * @param subscription
     */
    @Override
    public void expire(Subscription subscription) {
        throw new BusinessException(SubscriptionErrorCode.ERR_SUBSCRIPTION_ALREADY_EXPIRED);
    }

    /**
     * 만료 상태에서 재갱신
     * @param subscription
     */
    @Override
    public void renew(Subscription subscription) {
        throw new BusinessException(SubscriptionErrorCode.ERR_SUBSCRIPTION_ALREADY_EXPIRED);
    }
}