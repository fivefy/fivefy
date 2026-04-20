package com.fivefy.common.lock.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedissonLock {

    String key();

    // 락 획득 최대 대기 시간
    long retryDelaySeconds() default 10;

    // 락 TTL
    long lockTimeoutSeconds() default 5;

    // 시간 단위 기본: 초
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}