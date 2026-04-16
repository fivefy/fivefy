package com.fivefy.domain.popularchart.controller;

import com.fivefy.domain.popularchart.service.PopularChartGenerateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

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
