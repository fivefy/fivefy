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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
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

    @Nested
    @DisplayName("최근 검색 기록 조회")
    class GetRecentSearchHistories {

        @Test
        @DisplayName("최근 검색 기록을 Redis에서 조회한다")
        void getRecentSearchHistories_returnsFromRedis() {
            // given
            given(redisTemplate.opsForList()).willReturn(listOperations);
            given(listOperations.range(REDIS_KEY, 0, -1))
                    .willReturn(List.of("아이유", "루나", "뉴진스"));

            // when
            List<String> result = searchHistoryService.getRecentSearchHistories(USER_ID);

            // then
            assertThat(result).hasSize(3);
            assertThat(result).containsExactly("아이유", "루나", "뉴진스");
        }

        @Test
        @DisplayName("userId null 시 빈 리스트를 반환한다")
        void getRecentSearchHistories_nullUserId_returnsEmptyList() {
            // when
            List<String> result = searchHistoryService.getRecentSearchHistories(null);

            // then
            assertThat(result).isEmpty();
            verify(redisTemplate, never()).opsForList();
        }
    }

    @Nested
    @DisplayName("검색 기록 삭제")
    class DeleteSearchHistory {

        @Test
        @DisplayName("개별 검색 기록을 Redis에서 삭제한다")
        void deleteRecentSearchHistory_removesFromRedis() {
            // given
            given(redisTemplate.opsForList()).willReturn(listOperations);

            // when
            searchHistoryService.deleteRecentSearchHistory(USER_ID, KEYWORD);

            // then
            verify(listOperations, times(1)).remove(REDIS_KEY, 0, KEYWORD);
        }

        @Test
        @DisplayName("전체 검색 기록을 Redis에서 삭제한다")
        void deleteAllRecentSearchHistories_deletesKeyFromRedis() {
            // when
            searchHistoryService.deleteAllRecentSearchHistories(USER_ID);

            // then
            verify(redisTemplate, times(1)).delete(REDIS_KEY);
        }
    }
}