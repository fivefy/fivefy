package com.fivefy.domain.search.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.search.dto.response.SearchHistoryGetResponse;
import com.fivefy.domain.search.entity.SearchHistory;
import com.fivefy.domain.search.enums.SearchErrorCode;
import com.fivefy.domain.search.repository.SearchHistoryRepository;
import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.enums.UserErrorCode;
import com.fivefy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchHistoryService {

    private static final int MAX_SEARCH_HISTORY = 500;

    private final SearchHistoryRepository searchHistoryRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSearchHistory(Long userId, String keyword, Integer resultCount) {
        User user = getUser(userId);

        searchHistoryRepository.findByUserIdAndKeyword(user.getId(), keyword)
                .ifPresentOrElse(
                        existing -> existing.updateSearchedAt(resultCount),
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

    @Transactional(readOnly = true)
    public Page<SearchHistoryGetResponse> getSearchHistories(Long userId, Pageable pageable) {
        User user = getUser(userId);
        return searchHistoryRepository.findByUserId(user.getId(), pageable)
                .map(SearchHistoryGetResponse::from);
    }

    @Transactional
    public void deleteSearchHistories(Long userId) {
        User user = getUser(userId);
        searchHistoryRepository.deleteAllByUserId(user.getId());
    }

    @Transactional
    public void deleteSearchHistory(Long userId, Long historyId) {
        User user = getUser(userId);
        SearchHistory searchHistory = searchHistoryRepository.findByIdAndUserId(historyId, user.getId())
                .orElseThrow(() -> new BusinessException(SearchErrorCode.ERR_SEARCH_NOT_FOUND));
        searchHistoryRepository.delete(searchHistory);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.ERR_USER_NOT_FOUND));
    }
}
