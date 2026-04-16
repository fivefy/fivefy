package com.fivefy.domain.search.entity;

import com.fivefy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

@Getter
@Entity
@Table(
        name = "search_histories",
        indexes = {
                @Index(name = "idx_search_histories_user_searched", columnList = "user_id, searched_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "keyword"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String keyword;

    private Integer resultCount;

    @Column(nullable = false)
    private LocalDateTime searchedAt;

    public static SearchHistory create(Long userId, String keyword, Integer resultCount) {
        validateNonNull(userId, "userId");
        validateNonNull(keyword, "keyword");

        SearchHistory searchHistory = new SearchHistory();
        searchHistory.userId = userId;
        searchHistory.keyword = keyword;
        searchHistory.resultCount = resultCount;
        searchHistory.searchedAt = LocalDateTime.now();

        return searchHistory;
    }

    public void updateSearchedAt(Integer resultCount) {
        this.searchedAt = LocalDateTime.now();
        this.resultCount = resultCount;
    }
}
