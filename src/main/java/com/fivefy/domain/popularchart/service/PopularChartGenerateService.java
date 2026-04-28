package com.fivefy.domain.popularchart.service;

import com.fivefy.domain.playback.repository.PlaybackRepository;
import com.fivefy.domain.popularchart.dto.projection.TrackPlayCountProjection;
import com.fivefy.domain.popularchart.entity.PopularChart;
import com.fivefy.domain.popularchart.repository.PopularChartRepository;
import com.fivefy.domain.popularchart.utils.PopularChartDateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PopularChartGenerateService {

    // 유효 재생 기준 (30초 이상 재생된 경우만 집계)
    private static final int MINIMUM_VALID_PLAY_SECONDS = 30;
    // Top100 차트 제한
    private static final int TOP_CHART_LIMIT = 100;

    private final PlaybackRepository playbackRepository;
    private final PopularChartRepository popularChartRepository;

    @Transactional
    public void generateWeeklyChart(LocalDate date) {
        // 1. 기준 날짜를 해당 주 월요일(snapshotDate)로 보정
        LocalDate snapshotDate = PopularChartDateUtils.getSnapshotMonday(date);
        LocalDateTime snapshotDateTime = snapshotDate.atStartOfDay();

        // 2. 집계 구간 설정 (이전 주 월요일 00:00 ~ 해당 주 월요일 00:00)
        LocalDateTime startDateTime = snapshotDateTime.minusWeeks(1);

        log.info(
                "주간 인기 차트 생성 시작 - startDateTime={}, snapshotDateTime={}",
                startDateTime,
                snapshotDateTime
        );

        // 3. 유효 재생 기준으로 Playback 집계
        // playedDuration >= 30초
        // 동일 sessionId + trackId는 1회만 집계 (DISTINCT)
        // 종료된 재생(STOPPED, SKIPPED, COMPLETED)만 포함
        List<TrackPlayCountProjection> results = playbackRepository.countWeeklyValidPlayByTrack(
                startDateTime,
                snapshotDateTime,
                MINIMUM_VALID_PLAY_SECONDS,
                TOP_CHART_LIMIT
        );

        // 4. 집계 결과가 없으면 종료
        if (results.isEmpty()) {
            log.info("주간 인기 차트 생성 종료 - 집계 결과 없음, snapshotDateTime={}", snapshotDateTime);
            return;
        }

        // 5. 기존 차트가 존재하면 삭제 (snapshot 덮어쓰기)
        if (popularChartRepository.existsBySnapshotDate(snapshotDateTime)) {
            popularChartRepository.deleteAllBySnapshotDate(snapshotDateTime);
            log.info("기존 주간 인기 차트 삭제 완료 - snapshotDateTime={}", snapshotDateTime);
        }

        // 6. Top100 차트 생성
        List<PopularChart> charts = createTopCharts(results, snapshotDateTime);

        // 7. 차트 저장
        popularChartRepository.saveAllAndFlush(charts);

        log.info(
                "주간 인기 차트 생성 완료 - snapshotDateTime={}, chartCount={}",
                snapshotDateTime,
                charts.size()
        );
    }

    // 집계 결과를 기반으로 Top100 차트 엔티티 생성
    private List<PopularChart> createTopCharts(
            List<TrackPlayCountProjection> results,
            LocalDateTime snapshotDateTime
    ) {
        List<PopularChart> charts = new ArrayList<>();

        int rank = 1;

        // 집계 결과를 순위 기반으로 Top100까지 차트 생성
        for (TrackPlayCountProjection result : results) {
            charts.add(
                    PopularChart.create(
                            result.getTrackId(),   // 트랙 ID
                            rank++,                // 순위
                            result.getPlayCount(), // 재생 수
                            snapshotDateTime       // 기준 snapshotDate
                    )
            );
        }

        return charts;
    }
}
