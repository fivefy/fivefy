package com.fivefy.domain.popularchart.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.popularchart.dto.response.PopularChartResponse;
import com.fivefy.domain.popularchart.entity.PopularChart;
import com.fivefy.domain.popularchart.enums.PopularChartErrorCode;
import com.fivefy.domain.popularchart.repository.PopularChartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopularChartService {

    private final PopularChartRepository popularChartRepository;

    public List<PopularChartResponse> getTop100(LocalDate snapshotDate) {
        LocalDateTime targetDate;
        if (snapshotDate == null) {
            targetDate = findLatestSnapshotDate();
        } else {
            targetDate = snapshotDate.atStartOfDay();
        }

        List<PopularChart> charts =
                popularChartRepository.findTop100BySnapshotDateOrderByChartRankAsc(targetDate);
        if (charts.isEmpty()) {
            throw new BusinessException(PopularChartErrorCode.CHART_NOT_FOUND);
        }

        return charts.stream()
                .map(PopularChartResponse::from)
                .toList();
    }

    private LocalDateTime findLatestSnapshotDate() {
        return popularChartRepository.findAll().stream()
                .map(PopularChart::getSnapshotDate)
                .max(LocalDateTime::compareTo)
                .orElseThrow(() -> new BusinessException(PopularChartErrorCode.CHART_NOT_FOUND));
    }
}
