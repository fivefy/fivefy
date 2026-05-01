package com.fivefy.domain.subscription.state;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.subscription.entity.Subscription;
import com.fivefy.domain.subscription.enums.*;

public class CanceledState implements SubscriptionState {

    // 클래스 로드 시 객체 생성
    public static final CanceledState INSTANCE = new CanceledState();

    // 외부에서 new 못하게 막음
    private CanceledState() {}

    /**
     * 이미 취소된 상태 → 재취소 불가
     */
    @Override
    public void cancel(Subscription subscription) {
        throw new BusinessException(SubscriptionErrorCode.ERR_SUBSCRIPTION_INVALID_STATUS_CANCEL);
    }

    /**
     * 취소 → 만료
     * - 취소 후 기간 종료 시 만료 처리
     * - 취소 = 정기 구독 취소(다음 갱신일 null)
     */
    @Override
    public void expire(Subscription subscription) {
        subscription.changeStatus(SubscriptionStatus.EXPIRE);
    }

    /**
     * 취소된 구독은 갱신 불가
     */
    @Override
    public void renew(Subscription subscription) {
        throw new BusinessException(SubscriptionErrorCode.ERR_SUBSCRIPTION_ALREADY_CANCELED);
    }
}