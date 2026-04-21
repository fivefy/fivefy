package com.fivefy.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class LastActiveAtFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    private static final String LAST_ACTIVE_PREFIX = "lastActive:";
    private static final String THROTTLE_PREFIX = "lastActive:throttle:";
    // 30일 판단 기준보다 여유있게 35일 TTL
    private static final Duration TTL = Duration.ofDays(35);
    // 5분 이내 재요청은 Redis 쓰기 skip
    private static final Duration THROTTLE_TTL = Duration.ofMinutes(5);

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // 인증된 요청에서만 기록 (익명 요청 제외)
            if (authentication != null
                    && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof Long userId) {

                // SETNX로 5분 throttle — 최초 요청일 때만 lastActive 기록
                Boolean isFirst = redisTemplate.opsForValue()
                        .setIfAbsent(THROTTLE_PREFIX + userId, "1", THROTTLE_TTL);

                if (Boolean.TRUE.equals(isFirst)) {
                    // UTC 기준 Instant로 저장 — 서버 타임존 변경에 안전
                    redisTemplate.opsForValue().set(
                            LAST_ACTIVE_PREFIX + userId,
                            Instant.now().toString(),
                            TTL
                    );
                }
            }
        } catch (Exception e) {
            // lastActiveAt 기록 실패는 요청 처리에 영향 주지 않음
            log.warn("lastActiveAt 기록 실패 : {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
