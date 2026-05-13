package com.fivefy.domain.track.service;

import com.fivefy.domain.track.dto.cache.TrackDetailCache;
import com.fivefy.domain.track.enums.TrackType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * TrackDetailCacheService의 캐시 조회/저장 로직을 검증하는 단위 테스트
 *
 * 트랙 상세 캐시 조회 기능 검증
 * 트랙 상세 캐시 저장 기능 검증
 * 트랙 상세 캐시 조회 또는 생성 기능 검증
 * 트랙 상세 캐시 제거 기능 검증
 */
@ExtendWith(MockitoExtension.class)
class TrackDetailCacheServiceTest {

    private static final String CACHE_KEY = "track:detail:1";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TrackDetailCacheService trackDetailCacheService;

    @Nested
    @DisplayName("트랙 상세 캐시 조회")
    class Get {

        @Test
        @DisplayName("트랙 상세 캐시 조회 성공")
        void get_success() throws Exception {
            Long trackId = 1L;
            String cacheValue = "{\"trackId\":1}";
            TrackDetailCache cache = createTrackDetailCache(trackId);

            ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(CACHE_KEY)).thenReturn(cacheValue);
            when(objectMapper.readValue(cacheValue, TrackDetailCache.class)).thenReturn(cache);

            Optional<TrackDetailCache> response = trackDetailCacheService.get(trackId);

            assertThat(response).isPresent();
            assertThat(response.get().trackId()).isEqualTo(trackId);
            assertThat(response.get().title()).isEqualTo("밤편지");
        }

        @Test
        @DisplayName("캐시 값이 없으면 빈 Optional 반환")
        void get_empty_whenCacheMiss() {
            Long trackId = 1L;

            ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(CACHE_KEY)).thenReturn(null);

            Optional<TrackDetailCache> response = trackDetailCacheService.get(trackId);

            assertThat(response).isEmpty();
        }

        @Test
        @DisplayName("캐시 역직렬화 실패 시 캐시 제거 후 빈 Optional 반환")
        void get_emptyAndEvict_whenDeserializeFail() throws Exception {
            Long trackId = 1L;
            String cacheValue = "invalid-cache-value";

            ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(CACHE_KEY)).thenReturn(cacheValue);
            when(objectMapper.readValue(cacheValue, TrackDetailCache.class))
                    .thenThrow(new RuntimeException("역직렬화 실패"));

            Optional<TrackDetailCache> response = trackDetailCacheService.get(trackId);

            assertThat(response).isEmpty();
            verify(stringRedisTemplate).delete(CACHE_KEY);
        }

        @Test
        @DisplayName("Redis 조회 실패 시 빈 Optional 반환")
        void get_empty_whenRedisGetFail() {
            Long trackId = 1L;

            when(stringRedisTemplate.opsForValue())
                    .thenThrow(new RuntimeException("Redis 조회 실패"));

            Optional<TrackDetailCache> response = trackDetailCacheService.get(trackId);

            assertThat(response).isEmpty();
        }
    }

    @Nested
    @DisplayName("트랙 상세 캐시 저장")
    class Put {

        @Test
        @DisplayName("트랙 상세 캐시 저장 성공")
        void put_success() throws Exception {
            Long trackId = 1L;
            TrackDetailCache cache = createTrackDetailCache(trackId);
            String cacheValue = "{\"trackId\":1}";

            ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(objectMapper.writeValueAsString(cache)).thenReturn(cacheValue);

            trackDetailCacheService.put(trackId, cache);

            verify(valueOperations).set(CACHE_KEY, cacheValue, CACHE_TTL);
        }

        @Test
        @DisplayName("캐시 직렬화 실패 시 예외를 전파하지 않음")
        void put_ignore_whenSerializeFail() throws Exception {
            Long trackId = 1L;
            TrackDetailCache cache = createTrackDetailCache(trackId);

            when(objectMapper.writeValueAsString(cache))
                    .thenThrow(new RuntimeException("직렬화 실패"));

            trackDetailCacheService.put(trackId, cache);

            verify(stringRedisTemplate, never()).opsForValue();
        }
    }

    @Nested
    @DisplayName("트랙 상세 캐시 조회 또는 생성")
    class GetOrLoad {

        @Test
        @DisplayName("캐시가 있으면 loader를 실행하지 않고 캐시 반환")
        void getOrLoad_success_whenCacheHit() throws Exception {
            Long trackId = 1L;
            String cacheValue = "{\"trackId\":1}";
            TrackDetailCache cache = createTrackDetailCache(trackId);

            ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(CACHE_KEY)).thenReturn(cacheValue);
            when(objectMapper.readValue(cacheValue, TrackDetailCache.class)).thenReturn(cache);

            Supplier<TrackDetailCache> loader = mock(Supplier.class);

            TrackDetailCache response = trackDetailCacheService.getOrLoad(trackId, loader);

            assertThat(response.trackId()).isEqualTo(trackId);
            verify(loader, never()).get();
        }

        @Test
        @DisplayName("캐시가 없으면 loader 실행 후 캐시 저장")
        void getOrLoad_success_whenCacheMiss() throws Exception {
            Long trackId = 1L;
            TrackDetailCache cache = createTrackDetailCache(trackId);
            String cacheValue = "{\"trackId\":1}";

            ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(CACHE_KEY)).thenReturn(null);
            when(objectMapper.writeValueAsString(cache)).thenReturn(cacheValue);

            Supplier<TrackDetailCache> loader = mock(Supplier.class);
            when(loader.get()).thenReturn(cache);

            TrackDetailCache response = trackDetailCacheService.getOrLoad(trackId, loader);

            assertThat(response.trackId()).isEqualTo(trackId);
            verify(loader).get();
            verify(valueOperations).set(CACHE_KEY, cacheValue, CACHE_TTL);
        }
    }

    @Nested
    @DisplayName("트랙 상세 캐시 제거")
    class Evict {

        @Test
        @DisplayName("트랙 상세 캐시 제거 성공")
        void evict_success() {
            Long trackId = 1L;

            trackDetailCacheService.evict(trackId);

            verify(stringRedisTemplate).delete(CACHE_KEY);
        }

        @Test
        @DisplayName("캐시 제거 실패 시 예외를 전파하지 않음")
        void evict_ignore_whenRedisDeleteFail() {
            Long trackId = 1L;

            when(stringRedisTemplate.delete(CACHE_KEY))
                    .thenThrow(new RuntimeException("Redis 삭제 실패"));

            trackDetailCacheService.evict(trackId);
        }
    }

    private TrackDetailCache createTrackDetailCache(Long trackId) {
        return new TrackDetailCache(
                trackId,
                TrackType.OFFICIAL_RELEASE,
                10L,
                "아이유",
                100L,
                "Palette",
                1L,
                "밤편지",
                "가사",
                "BALLAD",
                230L,
                "feat. 10cm",
                LocalDateTime.of(2026, 5, 1, 18, 0, 0)
        );
    }
}
