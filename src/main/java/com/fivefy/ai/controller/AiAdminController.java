package com.fivefy.ai.controller;

import com.fivefy.ai.job.TrackEmbeddingJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 임베딩 잡 수동 트리거 (관리자 전용).
 * 운영 중 신규 트랙이 대량 들어왔거나 임베딩 모델 교체 시 사용.
 * 보안: ROLE_ADMIN 필요 (SecurityConfig에 역할 추가 가정)
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AiAdminController {

    private final EmbeddingTriggerService trigger;

    @PostMapping("/embedding/tracks/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> runTrackEmbedding() {
        boolean accepted = trigger.tryStart();
        if (!accepted) {
            return ResponseEntity.status(409).body("Embedding job already running");
        }
        return ResponseEntity.accepted().body("Track embedding job started");
    }

    /**
     * @Async 적용을 위한 별도 빈.
     *  - 같은 클래스 안에서 @Async 메서드를 호출하면 self-invocation 으로 비동기가 안 걸림
     *    (→ TrackEmbeddingService 의 @Transactional 분리와 같은 이유)
     *  - 동시 실행 가드도 여기에 둠
     *
     * 비동기 실행기는 별도 ThreadPoolTaskExecutor 빈을 단일 스레드로 설정해두는 것을 권장.
     *   @Bean("embeddingTaskExecutor")
     *   Executor embeddingTaskExecutor() {
     *       ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
     *       executor.setCorePoolSize(1); executor.setMaxPoolSize(1);
     *       executor.setQueueCapacity(1); executor.setThreadNamePrefix("embedding-");
     *       executor.initialize();
     *       return executor;
     *   }
     */
    @Component
    @RequiredArgsConstructor
    static class EmbeddingTriggerService {

        private final TrackEmbeddingJob job;
        private final AtomicBoolean running = new AtomicBoolean(false);

        /**
         * 잡 실행을 시도하고, 이미 실행 중이면 false 반환.
         */
        boolean tryStart() {
            if (!running.compareAndSet(false, true)) {
                return false;
            }
            executeAsync();
            return true;
        }

        @Async("embeddingTaskExecutor")
        public void executeAsync() {
            try {
                job.runDailyEmbedding();
            } catch (Exception e) {
                // 비동기 스레드에서 던진 예외는 호출자에게 전달되지 않으므로 명시적 로깅
                log.error("Manual embedding job failed", e);
            } finally {
                running.set(false);
            }
        }
    }
}
