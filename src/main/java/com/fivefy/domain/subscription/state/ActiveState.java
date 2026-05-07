package com.fivefy.domain.subscription.state;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.subscription.entity.Subscription;
import com.fivefy.domain.subscription.enums.*;

/**
 *
 */
public class ActiveState implements SubscriptionState {

    // 싱글턴 방식 : 생성자가 여러 차례 호출되더라도 실제로 생성되는 객체는 하나
        // 최초 생성 이후에 호출된 생성자는 최초의 생성자가 생성한 객체를 리턴
    // 클래스 로드 시 객체 생성 : static 영역에 객체를 딱 1개만 생성
    public static final ActiveState INSTANCE = new ActiveState();
    // 외부에서 new로 객체 생성 못하게 막음
    private ActiveState() {}

    /**
     * ACTIVE → CANCELED
     * - FREE 플랜은 취소 불가
     * - 정상적인 취소 흐름
     */
    @Override
    public void cancel(Subscription subscription) {
        if (subscription.getPlanType() == SubscriptionPlanType.FREE) {
            throw new BusinessException(SubscriptionErrorCode.ERR_FREE_SUBSCRIPTION_CANNOT_CANCEL);
        }

        subscription.changeStatus(SubscriptionStatus.CANCELED);
        subscription.clearNextBillingDate(); // 자동 결제 중단
    }

    /**
     * ACTIVE → EXPIRE
     * - 만료 시 자동 결제 중단
     */
    @Override
    public void expire(Subscription subscription) {
        subscription.changeStatus(SubscriptionStatus.EXPIRE);
        subscription.clearNextBillingDate();
    }

    /**
     * ACTIVE 유지 + 기간 연장
     * - 재갱신은 정기 결제 플랜만 가능
     * - nextBillingDate 없으면 이미 취소된 상태
     */
    @Override
    public void renew(Subscription subscription) {
        if (subscription.getPlanType() != SubscriptionPlanType.RECURRING_AUTO) {
            throw new BusinessException(SubscriptionErrorCode.ERR_SUBSCRIPTION_NOT_RECURRING);
        }

        if (subscription.getNextBillingDate() == null) {
            throw new BusinessException(SubscriptionErrorCode.ERR_SUBSCRIPTION_ALREADY_CANCELED);
        }

        subscription.extendOneMonth();
    }
}