package com.fivefy.domain.search.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.search.entity.SearchHistory;
import com.fivefy.domain.search.repository.SearchHistoryRepository;
import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.enums.UserErrorCode;
import com.fivefy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchHistoryService {

    private static final int MAX_SEARCH_HISTORY = 20;

    private final SearchHistoryRepository searchHistoryRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSearchHistory(Long userId, String keyword, Integer resultCount) {
        User user = getUser(userId);

        searchHistoryRepository.findByUserIdAndKeyword(user.getId(), keyword)
                .ifPresentOrElse(
                        existing -> existing.updateSearchedAt(resultCount), // 기존 기록 갱신
                        () -> {
                            if (searchHistoryRepository.countByUserId(user.getId()) >= MAX_SEARCH_HISTORY) {
                                searchHistoryRepository.findTopByUserIdOrderBySearchedAtAsc(user.getId())
                                        .ifPresent(searchHistoryRepository::delete);
                            }
                            searchHistoryRepository.save(
                                    SearchHistory.create(user.getId(), keyword, resultCount));
                        }
                );
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.ERR_USER_NOT_FOUND));
    }
}
