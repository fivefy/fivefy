package com.fivefy.domain.popularchart.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public final class PopularChartDateUtils {

    private PopularChartDateUtils() {
    }

    public static LocalDate getSnapshotMonday(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
