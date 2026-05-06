package com.fivefy.ai.controller;

import com.fivefy.ai.job.LyricsEmbeddingJob;
import com.fivefy.ai.job.TrackEmbeddingJob;
import com.fivefy.common.dto.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<BaseResponse<Void>> runTrackEmbedding() {
        boolean accepted = trigger.tryStartTracks();
        if (!accepted) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    BaseResponse.fail(HttpStatus.CONFLICT, "처리중이거나 처리된 작업입니다", null)
            );
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                BaseResponse.success(HttpStatus.ACCEPTED, "트랙 임베딩 작업 시작", null)
        );
    }

    @PostMapping("/embedding/lyrics/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> runLyricsEmbedding() {
        boolean accepted = trigger.tryStartLyrics();
        if (!accepted) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    BaseResponse.fail(HttpStatus.CONFLICT, "가사 임베딩 작업이 이미 진행 중입니다", null)
            );
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                BaseResponse.success(HttpStatus.ACCEPTED, "가사 임베딩 작업 시작", null)
        );
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

        private final TrackEmbeddingJob trackJob;
        private final LyricsEmbeddingJob lyricsJob;

        // 트랙/가사 각각 독립적인 실행 가드
        private final AtomicBoolean tracksRunning = new AtomicBoolean(false);
        private final AtomicBoolean lyricsRunning = new AtomicBoolean(false);

        /**
         * 트랙 임베딩 잡 실행을 시도. 이미 실행 중이면 false.
         */
        boolean tryStartTracks() {
            if (!tracksRunning.compareAndSet(false, true)) {
                return false;
            }
            executeTracksAsync();
            return true;
        }

        /**
         * 가사 임베딩 잡 실행을 시도. 이미 실행 중이면 false.
         */
        boolean tryStartLyrics() {
            if (!lyricsRunning.compareAndSet(false, true)) {
                return false;
            }
            executeLyricsAsync();
            return true;
        }

        @Async("embeddingTaskExecutor")
        public void executeTracksAsync() {
            try {
                trackJob.runDailyEmbedding();
            } catch (Exception e) {
                // 비동기 스레드 예외는 호출자에 전파되지 않으므로 명시적 로깅
                log.error("Manual track embedding job failed", e);
            } finally {
                tracksRunning.set(false);
            }
        }

        @Async("embeddingTaskExecutor")
        public void executeLyricsAsync() {
            try {
                lyricsJob.runDailyEmbedding();
            } catch (Exception e) {
                log.error("Manual lyrics embedding job failed", e);
            } finally {
                lyricsRunning.set(false);
            }
        }
    }
}
