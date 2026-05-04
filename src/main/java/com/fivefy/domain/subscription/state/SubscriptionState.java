package com.fivefy.domain.subscription.state;

import com.fivefy.domain.subscription.entity.Subscription;

/**
 * 구독 상태별 행동 정의 인터페이스
 *
 * 핵심 역할:
 * - 상태마다 "가능한 행동"을 분리 : 취소, 만료, 갱신
 * - if문 제거
 * - 상태가 직접 자신의 행동을 책임지도록 설계
 */
public interface SubscriptionState {

    /**
     * 구독 취소
     */
    void cancel(Subscription subscription);

    /**
     * 구독 만료 처리
     */
    void expire(Subscription subscription);

    /**
     * 정기 구독 갱신
     */
    void renew(Subscription subscription);
}