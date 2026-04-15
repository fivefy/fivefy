package com.fivefy.domain.search.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.search.entity.SearchHistory;
import com.fivefy.domain.search.repository.SearchHistoryRepository;
import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.enums.UserErrorCode;
import com.fivefy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchHistoryService {

    private static final int MAX_SEARCH_HISTORY = 20;

    private final SearchHistoryRepository searchHistoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public void saveSearchHistory(Long userId, String keyword, Integer resultCount) {
        getUser(userId);

        // 중복 키워드 삭제
        searchHistoryRepository.deleteByUserIdAndKeyword(userId, keyword);

        // 20개 초과 시 가장 오래된 것 삭제
        if (searchHistoryRepository.countByUserId(userId) >= MAX_SEARCH_HISTORY) {
            searchHistoryRepository.findTopByUserIdOrderByCreatedAtAsc(userId)
                    .ifPresent(searchHistoryRepository::delete);
        }

        searchHistoryRepository.save(SearchHistory.create(userId, keyword, resultCount));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.ERR_USER_NOT_FOUND));
    }
}
