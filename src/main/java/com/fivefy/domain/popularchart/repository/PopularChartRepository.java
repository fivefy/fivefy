package com.fivefy.domain.popularchart.repository;

import com.fivefy.domain.popularchart.entity.PopularChart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PopularChartRepository extends JpaRepository<PopularChart, Long> {

    List<PopularChart> findTop100BySnapshotDateOrderByChartRankAsc(LocalDateTime snapshotDate);
    Optional<PopularChart> findFirstByOrderBySnapshotDateDesc();
    boolean existsBySnapshotDate(LocalDateTime snapshotDate);
    void deleteAllBySnapshotDate(LocalDateTime snapshotDate);
    long countBySnapshotDate(LocalDateTime snapshotDate);
}
