package com.fivefy.domain.search.service;

import com.fivefy.domain.search.entity.SearchHistory;
import com.fivefy.domain.search.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchHistoryService {

    private static final String RECENT_SEARCH_KEY = "search:recent:";
    private static final int MAX_RECENT_SEARCH = 10;
    private static final Duration RECENT_SEARCH_TTL = Duration.ofDays(30);

    private final SearchHistoryRepository searchHistoryRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final DefaultRedisScript<Long> PUSH_RECENT_SEARCH_SCRIPT =
            new DefaultRedisScript<>("""
                    local key     = KEYS[1]
                    local keyword = ARGV[1]
                    local max     = tonumber(ARGV[2])
                    local ttlSec  = tonumber(ARGV[3])
 
                    redis.call('LREM',   key, 0, keyword)
                    redis.call('LPUSH',  key, keyword)
                    redis.call('LTRIM',  key, 0, max - 1)
                    redis.call('EXPIRE', key, ttlSec)
 
                    return 1
                    """, Long.class);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSearchHistory(Long userId, String keyword, Integer resultCount) {
        // DB에 로그 저장
        searchHistoryRepository.save(SearchHistory.create(userId, keyword, resultCount));

        // Redis에 최근 기록 저장
        if (userId != null) {
            String key = RECENT_SEARCH_KEY + userId;
            redisTemplate.execute(
                    PUSH_RECENT_SEARCH_SCRIPT,
                    List.of(key),
                    keyword,
                    String.valueOf(MAX_RECENT_SEARCH),
                    String.valueOf(RECENT_SEARCH_TTL.toSeconds())
            );
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
