package com.fivefy.domain.search.repository;

import com.fivefy.domain.search.entity.SearchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    Optional<SearchHistory> findByUserIdAndKeyword(Long userId, String keyword);
    Long countByUserId(Long userId);
    Optional<SearchHistory> findTopByUserIdOrderBySearchedAtAsc(Long userId);
    Page<SearchHistory> findByUserId(Long userId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM SearchHistory s WHERE s.userId = :userId")
    void deleteAllByUserId(Long userId);

    Optional<SearchHistory> findByIdAndUserId(Long historyId, Long userId);
}
