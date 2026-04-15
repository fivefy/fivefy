package com.fivefy.domain.search.repository;

import com.fivefy.domain.search.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    void deleteByUserIdAndKeyword(Long userId, String keyword);

    long countByUserId(Long userId);

    Optional<SearchHistory> findTopByUserIdOrderByCreatedAtAsc(Long userId);
}
