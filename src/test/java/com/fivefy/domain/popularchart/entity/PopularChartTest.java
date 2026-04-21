package com.fivefy.domain.popularchart.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PopularChartTest {

    @Test
    @DisplayName("인기 차트 생성 성공")
    void create_success() {
        // given
        Long trackId = 101L;
        Integer chartRank = 1;
        Long playCount = 300L;
        LocalDateTime snapshotDate = LocalDateTime.of(2026, 4, 13, 0, 0);

        // when
        PopularChart chart = PopularChart.create(trackId, chartRank, playCount, snapshotDate);

        // then
        assertThat(chart.getTrackId()).isEqualTo(trackId);
        assertThat(chart.getChartRank()).isEqualTo(chartRank);
        assertThat(chart.getPlayCount()).isEqualTo(playCount);
        assertThat(chart.getSnapshotDate()).isEqualTo(snapshotDate);
    }

    @Test
    @DisplayName("인기 차트 생성 실패 - trackId가 null이면 예외 발생")
    void create_fail_trackId_null() {
        // given
        Integer chartRank = 1;
        Long playCount = 300L;
        LocalDateTime snapshotDate = LocalDateTime.of(2026, 4, 13, 0, 0);

        // when & then
        assertThatThrownBy(() -> PopularChart.create(null, chartRank, playCount, snapshotDate))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("trackId(은)는 필수입니다");
    }

    @Test
    @DisplayName("인기 차트 생성 실패 - rank가 null이면 예외 발생")
    void create_fail_rank_null() {
        // given
        Long trackId = 101L;
        Long playCount = 300L;
        LocalDateTime snapshotDate = LocalDateTime.of(2026, 4, 13, 0, 0);

        // when & then
        assertThatThrownBy(() -> PopularChart.create(trackId, null, playCount, snapshotDate))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("chartRank(은)는 필수입니다");
    }

    @Test
    @DisplayName("인기 차트 생성 실패 - playCount가 null이면 예외 발생")
    void create_fail_playCount_null() {
        // given
        Long trackId = 101L;
        Integer chartRank = 1;
        LocalDateTime snapshotDate = LocalDateTime.of(2026, 4, 13, 0, 0);

        // when & then
        assertThatThrownBy(() -> PopularChart.create(trackId, chartRank, null, snapshotDate))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("playCount(은)는 필수입니다");
    }

    @Test
    @DisplayName("인기 차트 생성 실패 - snapshotDate가 null이면 예외 발생")
    void create_fail_snapshotDate_null() {
        // given
        Long trackId = 101L;
        Integer chartRank = 1;
        Long playCount = 300L;

        // when & then
        assertThatThrownBy(() -> PopularChart.create(trackId, chartRank, playCount, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("snapshotDate(은)는 필수입니다");
    }
}
