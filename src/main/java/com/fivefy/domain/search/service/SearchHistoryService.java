package com.fivefy.domain.search.service;

import com.fivefy.domain.search.entity.SearchHistory;
import com.fivefy.domain.search.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchHistoryService {

    private static final String RECENT_SEARCH_KEY = "search:recent:";
    private static final int MAX_RECENT_SEARCH = 10;

    private final SearchHistoryRepository searchHistoryRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSearchHistory(Long userId, String keyword, Integer resultCount) {
        // DB에 로그 저장
        searchHistoryRepository.save(SearchHistory.create(userId, keyword, resultCount));

        // Redis에 최근 기록 저장
        if (userId != null) {
            String key = RECENT_SEARCH_KEY + userId;
            redisTemplate.opsForList().remove(key, 0, keyword); // 중복 제거
            redisTemplate.opsForList().leftPush(key, keyword);  // 맨 앞에 추가
            redisTemplate.opsForList().trim(key, 0, MAX_RECENT_SEARCH - 1); // 10개 유지
        }
    }

    public List<String> getRecentSearchHistories(Long userId) {
        if (userId == null)
            return List.of();

        String key = RECENT_SEARCH_KEY + userId;
        List<String> result = redisTemplate.opsForList().range(key, 0, -1);

        return result != null ? result : List.of();
    }

    public void deleteRecentSearchHistory(Long userId, String keyword) {
        if (userId == null)
            return;

        String key = RECENT_SEARCH_KEY + userId;
        redisTemplate.opsForList().remove(key, 0, keyword);
    }

    public void deleteAllRecentSearchHistories(Long userId) {
        if (userId == null)
            return;

        String key = RECENT_SEARCH_KEY + userId;
        redisTemplate.delete(key);
    }
}
