package com.fivefy.domain.popularchart.scheduler;

import com.fivefy.domain.popularchart.service.PopularChartGenerateService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 매주 월요일 00:00에 주간 인기 차트 snapshot 생성
 */
@Component
@RequiredArgsConstructor
public class PopularChartScheduler {

    private final PopularChartGenerateService popularChartGenerateService;

    @Scheduled(cron = "0 0 0 * * MON")
    public void generateWeeklyChart() {
        LocalDate today = LocalDate.now();
        popularChartGenerateService.generateWeeklyChart(today);
    }
}
