package com.fivefy.domain.search.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.domain.search.dto.response.SearchResponse;
import com.fivefy.domain.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api")
@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/search")
    public ResponseEntity<BaseResponse<SearchResponse>> search(
            @RequestParam String keyword,
            @PageableDefault(size = 20, sort = "relevance", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Long userId) {
        SearchResponse response = searchService.search(keyword, pageable, userId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK, "검색 성공", response));
    }
}