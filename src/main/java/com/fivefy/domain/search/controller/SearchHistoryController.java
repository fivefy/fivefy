package com.fivefy.domain.search.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.domain.search.dto.response.SearchHistoryGetResponse;
import com.fivefy.domain.search.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api")
@RestController
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    @GetMapping("/search-histories")
    public ResponseEntity<BaseResponse<Page<SearchHistoryGetResponse>>> getSearchHistories(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "searchedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<SearchHistoryGetResponse> responses = searchHistoryService.getSearchHistories(userId, pageable);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK, "검색 기록 조회 성공", responses));
    }

    @DeleteMapping("/search-histories")
    public ResponseEntity<BaseResponse> deleteSearchHistories(
            @AuthenticationPrincipal Long userId) {
        searchHistoryService.deleteSearchHistories(userId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK, "검색 기록 전체 삭제 성공", null));
    }

    @DeleteMapping("/search-histories/{historyId}")
    public ResponseEntity<BaseResponse> deleteSearchHistory(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long historyId) {
        searchHistoryService.deleteSearchHistory(userId, historyId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK, "검색 기록 삭제 성공", null));
    }
}
