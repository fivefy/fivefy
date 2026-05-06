package com.fivefy.ai.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiBusinessMetrics {

    private final MeterRegistry meterRegistry;

    public enum Feature {
        RECOMMENDATION, PLAYLIST, CHAT, MOOD_SEARCH
    }

    // ─── 호출 카운터 ───
    public void recordCall(Feature feature, boolean success) {
        Counter.builder("ai.feature.calls")
                .tag("feature", feature.name())
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .increment();
    }

    // ─── Cold start ───
    public void recordColdStart(Feature feature) {
        Counter.builder("ai.cold_start")
                .tag("feature", feature.name())
                .description("개인화 데이터 부족으로 fallback 발동")
                .register(meterRegistry)
                .increment();
    }

    // ─── 빈 결과 ───
    public void recordEmptyResult(Feature feature) {
        Counter.builder("ai.empty_result")
                .tag("feature", feature.name())
                .description("검색 결과가 0건")
                .register(meterRegistry)
                .increment();
    }

    // ─── 결과 개수 분포 ───
    public void recordResultCount(Feature feature, int count) {
        meterRegistry.summary("ai.result_count", "feature", feature.name())
                .record(count);
    }

    // ─── 단계별 지연 ───
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordLatency(Timer.Sample sample, Feature feature, String stage) {
        sample.stop(Timer.builder("ai.latency")
                .tag("feature", feature.name())
                .tag("stage", stage)
                .description("AI 기능 단계별 지연 시간")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry));
    }
}
