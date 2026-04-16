package com.fivefy.domain.search.repository;

import com.fivefy.domain.search.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    Optional<SearchHistory> findByUserIdAndKeyword(Long userId, String keyword);

    Long countByUserId(Long userId);

    Optional<SearchHistory> findTopByUserIdOrderBySearchedAtAsc(Long userId);
}
