package com.fivefy.domain.search.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.domain.search.dto.response.SearchResponse;
import com.fivefy.domain.search.service.SearchService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RequestMapping("/api")
@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/search")
    public ResponseEntity<BaseResponse<SearchResponse>> search(
            @RequestParam @NotBlank(message = "검색어를 입력해주세요")String keyword,
            @AuthenticationPrincipal Long userId) {
        SearchResponse response = searchService.search(keyword, userId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK, "검색 성공", response));
    }
}