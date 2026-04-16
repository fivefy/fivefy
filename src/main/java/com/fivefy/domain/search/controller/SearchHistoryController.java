package com.fivefy.domain.search.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.domain.search.dto.response.SearchHistoryGetResponse;
import com.fivefy.domain.search.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api")
@RestController
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    @GetMapping("/search-histories")
    public ResponseEntity<BaseResponse<List<SearchHistoryGetResponse>>> getSearchHistories(
            @AuthenticationPrincipal Long userId) {
        List<SearchHistoryGetResponse> responses = searchHistoryService.getSearchHistories(userId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK, "검색 기록 조회 성공", responses));
    }
}
