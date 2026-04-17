package com.fivefy.domain.search.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.domain.search.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api")
@RestController
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    @GetMapping("/search-histories")
    public ResponseEntity<BaseResponse<List<String>>> getRecentSearchHistories(
            @AuthenticationPrincipal Long userId) {
        List<String> responses = searchHistoryService.getRecentSearchHistories(userId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK, "최근 검색 기록 조회 성공", responses));
    }

    @DeleteMapping("/search-histories")
    public ResponseEntity<BaseResponse> deleteAllRecentSearchHistories(
            @AuthenticationPrincipal Long userId) {
        searchHistoryService.deleteAllRecentSearchHistories(userId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK, "최근 검색 기록 전체 삭제 성공", null));
    }

    @DeleteMapping("/search-histories/recent")
    public ResponseEntity<BaseResponse> deleteRecentSearchHistory(
            @AuthenticationPrincipal Long userId,
            @RequestParam String keyword) {
        searchHistoryService.deleteRecentSearchHistory(userId, keyword);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK, "최근 검색 기록 삭제 성공", null));
    }
}
