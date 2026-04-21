package com.fivefy.domain.popularchart.service;

import com.fivefy.domain.playback.repository.PlaybackRepository;
import com.fivefy.domain.popularchart.dto.projection.TrackPlayCountProjection;
import com.fivefy.domain.popularchart.entity.PopularChart;
import com.fivefy.domain.popularchart.repository.PopularChartRepository;
import com.fivefy.domain.popularchart.utils.PopularChartDateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopularChartGenerateService {

    // 유효 재생 기준 (30초 이상 재생된 경우만 집계)
    private static final int MINIMUM_VALID_PLAY_SECONDS = 30;

    private final PlaybackRepository playbackRepository;
    private final PopularChartRepository popularChartRepository;

    @Transactional
    public void generateWeeklyChart(LocalDate date) {
        // 1. 기준 날짜를 해당 주 월요일(snapshotDate)로 보정
        // 차트는 "주 단위 스냅샷" 기준으로 관리
        LocalDate snapshotDate = PopularChartDateUtils.getSnapshotMonday(date);

        // 2. 집계 구간 설정
        // 이전 주 월요일 00:00 ~ 해당 주 월요일 00:00
        LocalDateTime snapshotDateTime = snapshotDate.atStartOfDay();
        LocalDateTime startDateTime = snapshotDateTime.minusWeeks(1);

        // 개발 단계에서 로직 확인을 위한 로그 (운영 시 logger로 변경 예정)
        System.out.println("snapshotDateTime = " + snapshotDateTime);
        System.out.println("startDateTime = " + startDateTime);

        /**
         * 3. 유효 재생 기준을 반영한 Playback 집계
         * - playedDuration >= 30초
         * - 동일 sessionId + trackId는 1회만 집계
         * - 종료된 재생(STOPPED, SKIPPED, COMPLETED)만 포함
         */
        List<TrackPlayCountProjection> results =
                playbackRepository.countWeeklyValidPlayByTrack(
                        startDateTime,
                        snapshotDateTime,
                        MINIMUM_VALID_PLAY_SECONDS
                );

        System.out.println("results size = " + results.size());

        // 4. 집계 결과가 없으면 종료 (불필요한 DB 작업 방지)
        if (results.isEmpty()) {
            System.out.println("집계 결과 없음");
            return;
        }

        // 5. 동일 snapshotDate 데이터가 존재하면 삭제 후 재생성
        // 주간 차트는 snapshot 덮어쓰기 전략
        if (popularChartRepository.existsBySnapshotDate(snapshotDateTime)) {
            System.out.println("기존 차트 삭제");
            popularChartRepository.deleteAllBySnapshotDate(snapshotDateTime);
        }

        List<PopularChart> charts = new ArrayList<>();

        int rank = 1;

        // 6. 집계 결과 기반으로 차트 생성 (Top 100)
        for (TrackPlayCountProjection result : results.stream().limit(100).toList()) {
            System.out.println("trackId = " + result.getTrackId()
                    + ", playCount = " + result.getPlayCount());

            charts.add(
                    PopularChart.create(
                            result.getTrackId(),   // 트랙 ID
                            rank++,                // 순위
                            result.getPlayCount(), // 재생 수
                            snapshotDateTime       // 기준 snapshotDat
                    )
            );
        }

        System.out.println("charts size before save = " + charts.size());

        // 7. 차트 일괄 저장
        popularChartRepository.saveAllAndFlush(charts);

        System.out.println("차트 저장 완료");

        // 8. 저장 결과 확인 (디버깅용)
        long count = popularChartRepository.countBySnapshotDate(snapshotDateTime);
        System.out.println("현재 snapshotDate 차트 개수 = " + count);
    }
}
