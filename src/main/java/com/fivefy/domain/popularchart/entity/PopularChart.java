package com.fivefy.domain.popularchart.entity;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "popular_charts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_popular_chart_snapshot_track",
                        columnNames = {"snapshot_date", "track_id"}
                ),
                @UniqueConstraint(
                        name = "uk_popular_chart_snapshot_rank",
                        columnNames = {"snapshot_date", "chart_rank"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PopularChart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "track_id", nullable = false)
    private Long trackId;

    @Column(name = "chart_rank", nullable = false)
    private Integer chartRank;

    @Column(name = "play_count", nullable = false)
    private Long playCount;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDateTime snapshotDate;

    public static PopularChart create(Long trackId, Integer chartRank, Long playCount, LocalDateTime snapshotDate) {
        validateNonNull(trackId, "trackId");
        validateNonNull(chartRank, "chartRank");
        validateNonNull(playCount, "playCount");
        validateNonNull(snapshotDate, "snapshotDate");

        PopularChart chart = new PopularChart();
        chart.trackId = trackId;
        chart.chartRank = chartRank;
        chart.playCount = playCount;
        chart.snapshotDate = snapshotDate;

        return chart;
    }
}
