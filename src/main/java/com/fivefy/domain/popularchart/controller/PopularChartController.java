package com.fivefy.domain.popularchart.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.domain.popularchart.dto.response.PopularChartResponse;
import com.fivefy.domain.popularchart.service.PopularChartService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/popular-charts")
public class PopularChartController {

    private final PopularChartService popularChartService;

    @GetMapping("/top100")
    public ResponseEntity<BaseResponse<List<PopularChartResponse>>> getTop100(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate snapshotDate
    ) {
        List<PopularChartResponse> response = popularChartService.getTop100(snapshotDate);

        return ResponseEntity.ok(
                BaseResponse.success(HttpStatus.OK, "인기 차트 조회 성공", response)
        );
    }
}
