package com.fivefy.domain.search.dto.response;

import com.fivefy.domain.search.entity.SearchHistory;

import java.time.LocalDateTime;

public record SearchHistoryGetResponse(
        Long id,
        String keyword,
        Integer resultCount,
        LocalDateTime searchedAt
) {
    public static SearchHistoryGetResponse from(SearchHistory searchHistory) {
        return new SearchHistoryGetResponse(
                searchHistory.getId(),
                searchHistory.getKeyword(),
                searchHistory.getResultCount(),
                searchHistory.getSearchedAt()
        );
    }
}