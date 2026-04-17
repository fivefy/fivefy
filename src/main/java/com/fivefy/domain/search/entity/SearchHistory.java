package com.fivefy.domain.search.entity;

import com.fivefy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

@Getter
@Entity
@Table(
        name = "search_histories",
        indexes = {
                @Index(name = "idx_search_histories_user_id", columnList = "user_id"),
                @Index(name = "idx_search_histories_created_at", columnList = "created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(nullable = false)
    private String keyword;

    private Integer resultCount;

    public static SearchHistory create(Long userId, String keyword, Integer resultCount) {
        validateNonNull(keyword, "keyword");

        SearchHistory searchHistory = new SearchHistory();
        searchHistory.userId = userId;
        searchHistory.keyword = keyword;
        searchHistory.resultCount = resultCount;

        return searchHistory;
    }
}
