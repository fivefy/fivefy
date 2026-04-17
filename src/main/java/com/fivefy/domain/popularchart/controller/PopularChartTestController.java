package com.fivefy.domain.popularchart.controller;

import com.fivefy.domain.popularchart.service.PopularChartGenerateService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 주간 인기 차트 snapshot 생성을 수동으로 실행하기 위한 테스트용 API
 * (운영 환경에서는 스케줄러로 자동 생성되며, local/dev 환경에서만 사용)
 */
@Profile({"local", "dev"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class PopularChartTestController {

    private final PopularChartGenerateService popularChartGenerateService;

    @PostMapping("/popular-charts/generate")
    public String generate() {
        popularChartGenerateService.generateWeeklyChart(LocalDate.now());
        return "ok";
    }
}
