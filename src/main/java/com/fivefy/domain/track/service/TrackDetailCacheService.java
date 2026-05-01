package com.fivefy.domain.track.service;

import com.fivefy.domain.track.dto.cache.TrackDetailCache;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 트랙 상세 캐시 서비스
 */
@Service
@RequiredArgsConstructor
public class TrackDetailCacheService {

    private static final String TRACK_DETAIL_CACHE_KEY_PREFIX = "track:detail:";
    private static final Duration TRACK_DETAIL_CACHE_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 트랙 상세 캐시 조회
     */
    public Optional<TrackDetailCache> get(Long trackId) {
        String cacheValue = stringRedisTemplate.opsForValue().get(createKey(trackId));

        if (cacheValue == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(cacheValue, TrackDetailCache.class));
        } catch (Exception e) {
            evict(trackId);
            return Optional.empty();
        }
    }

    /**
     * 트랙 상세 캐시 저장
     */
    public void put(Long trackId, TrackDetailCache cache) {
        try {
            String cacheValue = objectMapper.writeValueAsString(cache);
            stringRedisTemplate.opsForValue().set(
                    createKey(trackId),
                    cacheValue,
                    TRACK_DETAIL_CACHE_TTL
            );
        } catch (Exception e) {
            // 캐시 저장 실패는 상세 조회 흐름을 막지 않음
        }
    }

    /**
     * 트랙 상세 캐시 조회 또는 생성
     */
    public TrackDetailCache getOrLoad(
            Long trackId,
            Supplier<TrackDetailCache> loader
    ) {
        return get(trackId)
                .orElseGet(() -> {
                    TrackDetailCache cache = loader.get();
                    put(trackId, cache);
                    return cache;
                });
    }

    /**
     * 트랙 상세 캐시 제거
     */
    public void evict(Long trackId) {
        stringRedisTemplate.delete(createKey(trackId));
    }

    // trackId 기준으로 트랙 상세 캐시 key 생성
    private String createKey(Long trackId) {
        return TRACK_DETAIL_CACHE_KEY_PREFIX + trackId;
    }
}