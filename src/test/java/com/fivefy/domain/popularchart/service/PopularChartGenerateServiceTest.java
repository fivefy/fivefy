package com.fivefy.domain.popularchart.service;

import com.fivefy.domain.playback.repository.PlaybackRepository;
import com.fivefy.domain.popularchart.dto.projection.TrackPlayCountProjection;
import com.fivefy.domain.popularchart.entity.PopularChart;
import com.fivefy.domain.popularchart.repository.PopularChartRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.fivefy.domain.popularchart.service.PopularChartGenerateService.TOP_CHART_LIMIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PopularChartGenerateServiceTest {

    private static final int MINIMUM_VALID_PLAY_SECONDS = 30;

    @InjectMocks
    private PopularChartGenerateService popularChartGenerateService;

    @Mock private PlaybackRepository playbackRepository;
    @Mock private PopularChartRepository popularChartRepository;

    @Nested
    @DisplayName("주간 인기 차트 생성")
    class GenerateWeeklyChart {

        @Test
        @DisplayName("집계 결과가 있으면 차트를 저장한다")
        void generateWeeklyChart_success() {
            // given
            LocalDate date = LocalDate.of(2026, 4, 16);
            LocalDateTime snapshotDateTime = LocalDate.of(2026, 4, 13).atStartOfDay();
            LocalDateTime startDateTime = snapshotDateTime.minusWeeks(1);

            List<TrackPlayCountProjection> results = List.of(
                    mockProjection(101L, 300L),
                    mockProjection(102L, 250L),
                    mockProjection(103L, 200L)
            );

            given(playbackRepository.countWeeklyValidPlayByTrack(
                    startDateTime,
                    snapshotDateTime,
                    MINIMUM_VALID_PLAY_SECONDS,
                    TOP_CHART_LIMIT
            )).willReturn(results);

            lenient().when(popularChartRepository.existsBySnapshotDate(snapshotDateTime))
                    .thenReturn(false);

            // when
            popularChartGenerateService.generateWeeklyChart(date);

            // then
            ArgumentCaptor<List<PopularChart>> captor = ArgumentCaptor.forClass(List.class);
            verify(popularChartRepository).saveAllAndFlush(captor.capture());

            List<PopularChart> savedCharts = captor.getValue();
            assertThat(savedCharts).hasSize(3);

            assertThat(savedCharts.get(0).getTrackId()).isEqualTo(101L);
            assertThat(savedCharts.get(0).getChartRank()).isEqualTo(1);
            assertThat(savedCharts.get(0).getPlayCount()).isEqualTo(300L);
            assertThat(savedCharts.get(0).getSnapshotDate()).isEqualTo(snapshotDateTime);

            assertThat(savedCharts.get(1).getTrackId()).isEqualTo(102L);
            assertThat(savedCharts.get(1).getChartRank()).isEqualTo(2);
            assertThat(savedCharts.get(1).getPlayCount()).isEqualTo(250L);

            assertThat(savedCharts.get(2).getTrackId()).isEqualTo(103L);
            assertThat(savedCharts.get(2).getChartRank()).isEqualTo(3);
            assertThat(savedCharts.get(2).getPlayCount()).isEqualTo(200L);

            verify(popularChartRepository, never()).deleteAllBySnapshotDate(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("집계 결과가 없으면 기존 차트를 삭제하고 저장하지 않고 종료한다")
        void generateWeeklyChart_emptyResult() {
            // given
            LocalDate date = LocalDate.of(2026, 4, 16);
            LocalDateTime snapshotDateTime = LocalDate.of(2026, 4, 13).atStartOfDay();
            LocalDateTime startDateTime = snapshotDateTime.minusWeeks(1);

            given(playbackRepository.countWeeklyValidPlayByTrack(
                    startDateTime,
                    snapshotDateTime,
                    MINIMUM_VALID_PLAY_SECONDS,
                    TOP_CHART_LIMIT
            )).willReturn(List.of());

            given(popularChartRepository.existsBySnapshotDate(snapshotDateTime))
                    .willReturn(true);

            // when
            popularChartGenerateService.generateWeeklyChart(date);

            // then
            verify(popularChartRepository).existsBySnapshotDate(snapshotDateTime);
            verify(popularChartRepository).deleteAllBySnapshotDate(snapshotDateTime);
            verify(popularChartRepository, never()).saveAllAndFlush(any());
        }

        @Test
        @DisplayName("집계 결과가 없고 기존 차트도 없으면 저장하지 않고 종료한다")
        void generateWeeklyChart_emptyResult_withoutExistingSnapshot() {
            // given
            LocalDate date = LocalDate.of(2026, 4, 16);
            LocalDateTime snapshotDateTime = LocalDate.of(2026, 4, 13).atStartOfDay();
            LocalDateTime startDateTime = snapshotDateTime.minusWeeks(1);

            given(playbackRepository.countWeeklyValidPlayByTrack(
                    startDateTime,
                    snapshotDateTime,
                    MINIMUM_VALID_PLAY_SECONDS,
                    TOP_CHART_LIMIT
            )).willReturn(List.of());

            given(popularChartRepository.existsBySnapshotDate(snapshotDateTime))
                    .willReturn(false);

            // when
            popularChartGenerateService.generateWeeklyChart(date);

            // then
            verify(popularChartRepository).existsBySnapshotDate(snapshotDateTime);
            verify(popularChartRepository, never()).deleteAllBySnapshotDate(any(LocalDateTime.class));
            verify(popularChartRepository, never()).saveAllAndFlush(any());
        }

        @Test
        @DisplayName("같은 snapshotDate 데이터가 이미 있으면 삭제 후 다시 저장한다")
        void generateWeeklyChart_deleteAndRegenerate() {
            // given
            LocalDate date = LocalDate.of(2026, 4, 16);
            LocalDateTime snapshotDateTime = LocalDate.of(2026, 4, 13).atStartOfDay();
            LocalDateTime startDateTime = snapshotDateTime.minusWeeks(1);

            List<TrackPlayCountProjection> results = List.of(
                    mockProjection(101L, 300L),
                    mockProjection(102L, 250L)
            );

            given(playbackRepository.countWeeklyValidPlayByTrack(
                    startDateTime,
                    snapshotDateTime,
                    MINIMUM_VALID_PLAY_SECONDS,
                    TOP_CHART_LIMIT
            )).willReturn(results);

            given(popularChartRepository.existsBySnapshotDate(snapshotDateTime))
                    .willReturn(true);

            // when
            popularChartGenerateService.generateWeeklyChart(date);

            // then
            verify(popularChartRepository).existsBySnapshotDate(snapshotDateTime);
            verify(popularChartRepository).deleteAllBySnapshotDate(snapshotDateTime);
            verify(popularChartRepository).saveAllAndFlush(any());
        }

        @Test
        @DisplayName("집계 쿼리에 Top100 제한 값을 전달한다")
        void generateWeeklyChart_passTopChartLimitToQuery() {
            // given
            LocalDate date = LocalDate.of(2026, 4, 16);
            LocalDateTime snapshotDateTime = LocalDate.of(2026, 4, 13).atStartOfDay();
            LocalDateTime startDateTime = snapshotDateTime.minusWeeks(1);

            given(playbackRepository.countWeeklyValidPlayByTrack(
                    startDateTime,
                    snapshotDateTime,
                    MINIMUM_VALID_PLAY_SECONDS,
                    TOP_CHART_LIMIT
            )).willReturn(List.of());

            // when
            popularChartGenerateService.generateWeeklyChart(date);

            // then
            verify(playbackRepository).countWeeklyValidPlayByTrack(
                    startDateTime,
                    snapshotDateTime,
                    MINIMUM_VALID_PLAY_SECONDS,
                    TOP_CHART_LIMIT
            );
        }

        private TrackPlayCountProjection mockProjection(Long trackId, Long playCount) {
            TrackPlayCountProjection projection = mock(TrackPlayCountProjection.class);
            lenient().when(projection.getTrackId()).thenReturn(trackId);
            lenient().when(projection.getPlayCount()).thenReturn(playCount);
            return projection;
        }
    }
}

