package com.fivefy.domain.popularchart.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * 입력된 날짜를 기준으로 해당 주의 월요일(snapshotDate)을 반환
 */
public final class PopularChartDateUtils {

    private PopularChartDateUtils() {
    }

    public static LocalDate getSnapshotMonday(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
