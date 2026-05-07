package com.fivefy.ai.controller;

import com.fivefy.ai.dto.request.MoodSearchRequest;
import com.fivefy.ai.dto.response.MoodSearchResponse;
import com.fivefy.ai.service.MoodSearchService;
import com.fivefy.common.dto.response.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/search/mood")
@RequiredArgsConstructor
public class MoodSearchController {

    private final MoodSearchService moodSearchService;

    @PostMapping
    public ResponseEntity<BaseResponse<MoodSearchResponse>> search(@Valid @RequestBody MoodSearchRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "무드 검색 성공", moodSearchService.search(request))
        );
    }
}
