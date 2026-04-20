package com.fivefy.common.lock.aspect;

import com.fivefy.common.lock.AopForTransaction;
import com.fivefy.common.lock.annotation.RedissonLock;
import com.fivefy.common.lock.consts.AopConsts;
import com.fivefy.common.lock.exception.ErrorEnum;
import com.fivefy.common.lock.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(AopConsts.LOCK_ASPECT_ORDER)
@RequiredArgsConstructor
@Slf4j
public class RedissonLockAspect {

    private final RedissonClient redissonClient;
    private final AopForTransaction aopForTransaction;  // 추가
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(redissonLock)")
    public Object run(ProceedingJoinPoint joinPoint, RedissonLock redissonLock) throws Throwable {

        // SpEL로 락 키 동적 추출
        String lockKey = resolveKey(joinPoint, redissonLock.key());

        // RLock은 락 키 단위로 관리되는 분산락 객체
        RLock lock = redissonClient.getLock(lockKey);

        log.info("[RedissonLockAspect] 락 획득 시도 - key: {}", lockKey);

        // 락 획득 시도
        boolean acquired = lock.tryLock(redissonLock.retryDelaySeconds(), redissonLock.lockTimeoutSeconds(), redissonLock.timeUnit());

        if (!acquired) {
            // retryDelaySeconds 안에 락을 획득하지 못한 경우
            log.warn("[RedissonLockAspect] 락 획득 실패 - key: {}", lockKey);
            throw new ServiceException(ErrorEnum.LOCK_ACQUISITION_FAILED);
        }

        log.info("[RedissonLockAspect] 락 획득 성공 - key: {}", lockKey);

        try {

            return aopForTransaction.proceed(joinPoint);

        } finally {
            // isHeldByCurrentThread(): 현재 스레드가 보유한 락인지 확인 후 해제
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("[RedissonLockAspect] 락 해제 성공 - key: {}", lockKey);
            } else {
                log.warn("[RedissonLockAspect] 락 이미 해제됨 - key: {}", lockKey);
            }
        }
    }

    /**
     * SpEL 표현식에서 실제 락 키 값을 동적 추출
     */
    private String resolveKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        String[] paramNames = signature.getParameterNames();

        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        return parser.parseExpression(keyExpression).getValue(context, String.class);
    }

}