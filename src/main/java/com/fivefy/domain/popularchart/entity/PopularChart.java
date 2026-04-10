package com.fivefy.domain.popularchart.entity;

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
    private int rank;

    @Column(nullable = false)
    private Long playCount;

    @Column(nullable = false)
    private LocalDateTime snapshotDate;

    public static PopularChart create(Long trackId, int rank, Long playCount, LocalDateTime snapshotDate) {
        PopularChart chart = new PopularChart();
        chart.trackId = trackId;
        chart.rank = rank;
        chart.playCount = playCount;
        chart.snapshotDate = snapshotDate;

        return chart;
    }
}
