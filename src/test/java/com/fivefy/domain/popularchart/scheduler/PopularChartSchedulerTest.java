package com.fivefy.domain.popularchart.scheduler;

import com.fivefy.domain.popularchart.service.PopularChartGenerateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PopularChartSchedulerTest {

    @InjectMocks
    private PopularChartScheduler popularChartScheduler;

    @Mock private PopularChartGenerateService popularChartGenerateService;

    @Test
    @DisplayName("주간 인기 차트 스케줄러 실행 성공")
    void generateWeeklyChart_success() {
        // when
        popularChartScheduler.generateWeeklyChart();

        // then
        verify(popularChartGenerateService, times(1))
                .generateWeeklyChart(any(LocalDate.class));
    }
}
