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
                @UniqueConstraint(name = "uk_snapshot_track", columnNames = {"snapshot_date", "track_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PopularChart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long trackId;

    @Column(nullable = false)
    private Integer chartRank;

    @Column(nullable = false)
    private Long playCount;

    @Column(nullable = false)
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
