package com.fivefy.domain.popularchart.dto.response;

import com.fivefy.domain.popularchart.entity.PopularChart;

import java.time.LocalDateTime;

public record PopularChartResponse(
        Long id,
        Long trackId,
        Integer rank,
        Long playCount,
        LocalDateTime snapshotDate
) {
    public static PopularChartResponse from(PopularChart popularChart) {
        return new PopularChartResponse(
                popularChart.getId(),
                popularChart.getTrackId(),
                popularChart.getChartRank(),
                popularChart.getPlayCount(),
                popularChart.getSnapshotDate()
        );
    }
}
