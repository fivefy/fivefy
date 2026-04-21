package com.fivefy.common.lock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AopForTransaction {

    /**
     * REQUIRES_NEW: 기존 트랜잭션과 별개로 새 트랜잭션 시작
     * proceed() 완료 시 커밋 → 이후 락 해제
     * 락 안에서 트랜잭션을 시작·커밋한 뒤 락을 해제해야
     * 다른 요청이 최신 데이터를 읽을 수 있음
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Object proceed(ProceedingJoinPoint joinPoint) throws Throwable {
        return joinPoint.proceed();
    }
}