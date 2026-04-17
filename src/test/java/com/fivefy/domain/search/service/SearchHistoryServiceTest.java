package com.fivefy.domain.search.service;

import com.fivefy.domain.search.repository.SearchHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchHistoryServiceTest {

    @InjectMocks
    private SearchHistoryService searchHistoryService;

    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    private static final Long USER_ID = 1L;
    private static final String KEYWORD = "아이유";
    private static final String REDIS_KEY = "search:recent:" + USER_ID;

    @Nested
    @DisplayName("검색 기록 저장")
    class SaveSearchHistory {

        @Test
        @DisplayName("검색 기록 저장 시 DB에 INSERT 된다")
        void saveSearchHistory_savesToDB() {
            // given
            given(redisTemplate.opsForList()).willReturn(listOperations);

            // when
            searchHistoryService.saveSearchHistory(USER_ID, KEYWORD, 10);

            // then
            verify(searchHistoryRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("검색 기록 저장 시 Redis에 leftPush 된다")
        void saveSearchHistory_leftPushToRedis() {
            // given
            given(redisTemplate.opsForList()).willReturn(listOperations);

            // when
            searchHistoryService.saveSearchHistory(USER_ID, KEYWORD, 10);

            // then
            verify(listOperations, times(1)).leftPush(REDIS_KEY, KEYWORD);
        }

        @Test
        @DisplayName("Redis 최근 기록 10개 초과 시 trim 된다")
        void saveSearchHistory_trimAfterPush() {
            // given
            given(redisTemplate.opsForList()).willReturn(listOperations);

            // when
            searchHistoryService.saveSearchHistory(USER_ID, KEYWORD, 10);

            // then
            verify(listOperations, times(1)).trim(REDIS_KEY, 0, 9);
        }

        @Test
        @DisplayName("중복 키워드 저장 시 remove 후 leftPush 된다")
        void saveSearchHistory_removeThenLeftPushForDuplicate() {
            // given
            given(redisTemplate.opsForList()).willReturn(listOperations);

            // when
            searchHistoryService.saveSearchHistory(USER_ID, KEYWORD, 10);

            // then
            verify(listOperations, times(1)).remove(REDIS_KEY, 0, KEYWORD);
            verify(listOperations, times(1)).leftPush(REDIS_KEY, KEYWORD);
        }

        @Test
        @DisplayName("userId null 시 Redis 저장이 스킵된다")
        void saveSearchHistory_nullUserId_skipsRedis() {
            // when
            searchHistoryService.saveSearchHistory(null, KEYWORD, 10);

            // then
            verify(redisTemplate, never()).opsForList();
            verify(searchHistoryRepository, times(1)).save(any()); // DB는 저장
        }
    }
}
