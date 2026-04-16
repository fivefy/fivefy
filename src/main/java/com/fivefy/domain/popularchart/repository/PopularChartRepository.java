package com.fivefy.domain.popularchart.repository;

import com.fivefy.domain.popularchart.entity.PopularChart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PopularChartRepository extends JpaRepository<PopularChart, Long> {

    List<PopularChart> findTop100BySnapshotDateOrderByChartRankAsc(LocalDateTime snapshotDate);
}
