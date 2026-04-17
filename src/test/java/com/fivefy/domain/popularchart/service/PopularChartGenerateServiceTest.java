package com.fivefy.domain.popularchart.service;

import com.fivefy.domain.playback.dto.projection.TrackPlayCountDto;
import com.fivefy.domain.playback.repository.PlaybackRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PopularChartGenerateServiceTest {

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
            LocalDate snapshotDate = LocalDate.of(2026, 4, 13);
            LocalDateTime snapshotDateTime = snapshotDate.atStartOfDay();
            LocalDateTime startDateTime = snapshotDateTime.minusWeeks(1);

            List<TrackPlayCountDto> results = List.of(
                    new TrackPlayCountDto(101L, 300L),
                    new TrackPlayCountDto(102L, 250L),
                    new TrackPlayCountDto(103L, 200L)
            );

            given(playbackRepository.countWeeklyPlayByTrack(startDateTime, snapshotDateTime))
                    .willReturn(results);
            given(popularChartRepository.existsBySnapshotDate(snapshotDateTime))
                    .willReturn(false);

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

            assertThat(savedCharts.get(2).getTrackId()).isEqualTo(103L);
            assertThat(savedCharts.get(2).getChartRank()).isEqualTo(3);

            verify(popularChartRepository, never()).deleteAllBySnapshotDate(any());
        }

        @Test
        @DisplayName("집계 결과가 없으면 저장하지 않고 종료한다")
        void generateWeeklyChart_emptyResult() {
            // given
            LocalDate date = LocalDate.of(2026, 4, 16);
            LocalDate snapshotDate = LocalDate.of(2026, 4, 13);
            LocalDateTime snapshotDateTime = snapshotDate.atStartOfDay();
            LocalDateTime startDateTime = snapshotDateTime.minusWeeks(1);

            given(playbackRepository.countWeeklyPlayByTrack(startDateTime, snapshotDateTime))
                    .willReturn(List.of());

            // when
            popularChartGenerateService.generateWeeklyChart(date);

            // then
            verify(popularChartRepository, never()).existsBySnapshotDate(any());
            verify(popularChartRepository, never()).deleteAllBySnapshotDate(any());
            verify(popularChartRepository, never()).saveAllAndFlush(any());
        }

        @Test
        @DisplayName("같은 snapshotDate 데이터가 이미 있으면 삭제 후 다시 저장한다")
        void generateWeeklyChart_deleteAndRegenerate() {
            // given
            LocalDate date = LocalDate.of(2026, 4, 16);
            LocalDate snapshotDate = LocalDate.of(2026, 4, 13);
            LocalDateTime snapshotDateTime = snapshotDate.atStartOfDay();
            LocalDateTime startDateTime = snapshotDateTime.minusWeeks(1);

            List<TrackPlayCountDto> results = List.of(
                    new TrackPlayCountDto(101L, 300L),
                    new TrackPlayCountDto(102L, 250L)
            );

            given(playbackRepository.countWeeklyPlayByTrack(startDateTime, snapshotDateTime))
                    .willReturn(results);
            given(popularChartRepository.existsBySnapshotDate(snapshotDateTime))
                    .willReturn(true);

            // when
            popularChartGenerateService.generateWeeklyChart(date);

            // then
            verify(popularChartRepository).deleteAllBySnapshotDate(snapshotDateTime);
            verify(popularChartRepository).saveAllAndFlush(any());
        }

        @Test
        @DisplayName("집계 결과가 100개를 초과하면 상위 100개만 저장한다")
        void generateWeeklyChart_saveOnlyTop100() {
            // given
            LocalDate date = LocalDate.of(2026, 4, 16);
            LocalDate snapshotDate = LocalDate.of(2026, 4, 13);
            LocalDateTime snapshotDateTime = snapshotDate.atStartOfDay();
            LocalDateTime startDateTime = snapshotDateTime.minusWeeks(1);

            List<TrackPlayCountDto> results = java.util.stream.LongStream.rangeClosed(1, 120)
                    .mapToObj(i -> new TrackPlayCountDto(i, 200L - i))
                    .toList();

            given(playbackRepository.countWeeklyPlayByTrack(startDateTime, snapshotDateTime))
                    .willReturn(results);
            given(popularChartRepository.existsBySnapshotDate(snapshotDateTime))
                    .willReturn(false);

            // when
            popularChartGenerateService.generateWeeklyChart(date);

            // then
            ArgumentCaptor<List<PopularChart>> captor = ArgumentCaptor.forClass(List.class);
            verify(popularChartRepository).saveAllAndFlush(captor.capture());

            List<PopularChart> savedCharts = captor.getValue();
            assertThat(savedCharts).hasSize(100);
            assertThat(savedCharts.get(0).getChartRank()).isEqualTo(1);
            assertThat(savedCharts.get(99).getChartRank()).isEqualTo(100);
        }
    }
}

