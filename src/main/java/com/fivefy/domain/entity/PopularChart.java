package com.fivefy.domain.entity;

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
    private Integer rank;

    @Column(nullable = false)
    private Long playCount;

    @Column(nullable = false)
    private LocalDateTime snapshotDate;

    public PopularChart(Long trackId, Integer rank, Long playCount, LocalDateTime snapshotDate) {
        this.trackId = trackId;
        this.rank = rank;
        this.playCount = playCount;
        this.snapshotDate = snapshotDate;
    }

    public static PopularChart create(Long trackId, Integer rank, Long playCount, LocalDateTime snapshotDate) {
        return new PopularChart(trackId, rank, playCount, snapshotDate);
    }

    public void updateChart(Integer rank, Long playCount) {
        this.rank = rank;
        this.playCount = playCount;
    }
}
