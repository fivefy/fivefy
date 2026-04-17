package com.fivefy.domain.popularchart.service;

import com.fivefy.domain.playback.dto.projection.TrackPlayCountDto;
import com.fivefy.domain.playback.repository.PlaybackRepository;
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

    private final PlaybackRepository playbackRepository;
    private final PopularChartRepository popularChartRepository;

    @Transactional
    public void generateWeeklyChart(LocalDate date) {
        // 기준 날짜를 해당 주 월요일(snapshotDate)로 보정
        LocalDate snapshotDate = PopularChartDateUtils.getSnapshotMonday(date);

        // 집계 구간: 이전 주 월요일 00:00 ~ 해당 주 월요일 00:00
        LocalDateTime snapshotDateTime = snapshotDate.atStartOfDay();
        LocalDateTime startDateTime = snapshotDateTime.minusWeeks(1);

        // 디버깅 및 집계 로직 검증을 위해 로그 출력 (추후 logger로 전환 예정)
        System.out.println("snapshotDateTime = " + snapshotDateTime);
        System.out.println("startDateTime = " + startDateTime);

        // 지난 1주일간의 track별 재생 수 집계
        List<TrackPlayCountDto> results =
                playbackRepository.countWeeklyPlayByTrack(startDateTime, snapshotDateTime);

        System.out.println("results size = " + results.size());

        // 집계 결과가 없으면 종료
        if (results.isEmpty()) {
            System.out.println("집계 결과 없음");
            return;
        }

        // 동일한 snapshotDate 데이터가 이미 있으면 삭제 후 재생성
        if (popularChartRepository.existsBySnapshotDate(snapshotDateTime)) {
            System.out.println("기존 차트 삭제");
            popularChartRepository.deleteAllBySnapshotDate(snapshotDateTime);
        }

        List<PopularChart> charts = new ArrayList<>();

        int rank = 1;
        for (TrackPlayCountDto result : results.stream().limit(100).toList()) {
            System.out.println("trackId = " + result.trackId() + ", playCount = " + result.playCount());

            charts.add(
                    PopularChart.create(
                            result.trackId(),
                            rank++,
                            result.playCount(),
                            snapshotDateTime
                    )
            );
        }

        System.out.println("charts size before save = " + charts.size());

        popularChartRepository.saveAllAndFlush(charts);

        System.out.println("차트 저장 완료");

        long count = popularChartRepository.countBySnapshotDate(snapshotDateTime);
        System.out.println("현재 snapshotDate 차트 개수 = " + count);
    }
}
