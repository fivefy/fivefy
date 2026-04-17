package com.fivefy.domain.popularchart.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.popularchart.dto.response.PopularChartResponse;
import com.fivefy.domain.popularchart.entity.PopularChart;
import com.fivefy.domain.popularchart.enums.PopularChartErrorCode;
import com.fivefy.domain.popularchart.repository.PopularChartRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PopularChartServiceTest {

    @InjectMocks
    private PopularChartService popularChartService;

    @Mock private PopularChartRepository popularChartRepository;

    @Nested
    @DisplayName("인기 차트 조회")
    class GetTop100 {

        @Test
        @DisplayName("snapshotDate가 있으면 해당 날짜 차트 조회 성공")
        void getTop100_withSnapshotDate_success() {
            // given
            LocalDate snapshotDate = LocalDate.of(2026, 4, 6);
            LocalDateTime snapshotDateTime = snapshotDate.atStartOfDay();

            PopularChart chart1 = PopularChart.create(101L, 1, 300L, snapshotDateTime);
            PopularChart chart2 = PopularChart.create(102L, 2, 250L, snapshotDateTime);

            ReflectionTestUtils.setField(chart1, "id", 1L);
            ReflectionTestUtils.setField(chart2, "id", 2L);

            given(popularChartRepository.findTop100BySnapshotDateOrderByChartRankAsc(snapshotDateTime))
                    .willReturn(List.of(chart1, chart2));

            // when
            List<PopularChartResponse> result = popularChartService.getTop100(snapshotDate);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).id()).isEqualTo(1L);
            assertThat(result.get(0).trackId()).isEqualTo(101L);
            assertThat(result.get(0).rank()).isEqualTo(1);
            assertThat(result.get(0).playCount()).isEqualTo(300L);

            assertThat(result.get(1).id()).isEqualTo(2L);
            assertThat(result.get(1).trackId()).isEqualTo(102L);
            assertThat(result.get(1).rank()).isEqualTo(2);
            assertThat(result.get(1).playCount()).isEqualTo(250L);
        }

        @Test
        @DisplayName("snapshotDate가 없으면 최신 차트 조회 성공")
        void getTop100_withoutSnapshotDate_success() {
            // given
            LocalDateTime latestSnapshotDate = LocalDateTime.of(2026, 4, 14, 0, 0);

            PopularChart latestChart = PopularChart.create(101L, 1, 300L, latestSnapshotDate);
            ReflectionTestUtils.setField(latestChart, "id", 1L);

            PopularChart chart1 = PopularChart.create(101L, 1, 300L, latestSnapshotDate);
            PopularChart chart2 = PopularChart.create(102L, 2, 250L, latestSnapshotDate);

            ReflectionTestUtils.setField(chart1, "id", 1L);
            ReflectionTestUtils.setField(chart2, "id", 2L);

            given(popularChartRepository.findFirstByOrderBySnapshotDateDesc())
                    .willReturn(Optional.of(latestChart));
            given(popularChartRepository.findTop100BySnapshotDateOrderByChartRankAsc(latestSnapshotDate))
                    .willReturn(List.of(chart1, chart2));

            // when
            List<PopularChartResponse> result = popularChartService.getTop100(null);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).trackId()).isEqualTo(101L);
            assertThat(result.get(0).rank()).isEqualTo(1);
            assertThat(result.get(1).trackId()).isEqualTo(102L);
            assertThat(result.get(1).rank()).isEqualTo(2);
        }

        @Test
        @DisplayName("최신 차트가 없으면 예외 발생")
        void getTop100_withoutSnapshotDate_chartNotFound() {
            // given
            given(popularChartRepository.findFirstByOrderBySnapshotDateDesc())
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> popularChartService.getTop100(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PopularChartErrorCode.CHART_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("해당 snapshotDate의 차트가 없으면 예외 발생")
        void getTop100_withSnapshotDate_chartNotFound() {
            // given
            LocalDate snapshotDate = LocalDate.of(2026, 4, 6);
            LocalDateTime snapshotDateTime = snapshotDate.atStartOfDay();

            given(popularChartRepository.findTop100BySnapshotDateOrderByChartRankAsc(snapshotDateTime))
                    .willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> popularChartService.getTop100(snapshotDate))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PopularChartErrorCode.CHART_NOT_FOUND.getMessage());
        }
    }
}
