package com.fivefy.common.lock.consts;

public class AopConsts {
    private AopConsts() {} // 인스턴스화 방지

    // 트랜젝션보다 먼저 락 발동
    public static final int LOCK_ASPECT_ORDER = 1;
}