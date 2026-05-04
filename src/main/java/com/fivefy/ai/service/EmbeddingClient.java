package com.fivefy.ai.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Ollama Embedding API 호출 래퍼 (bge-m3 모델, 1024차원).
 *
 * 왜 Ollama / bge-m3 인가:
 *  1) 비용 0 — 외부 API 비용 폭주 위험 없음
 *  2) 데이터 주권 — 가사 같은 민감 텍스트가 외부로 안 나감
 *  3) bge-m3 는 multilingual 지원 — 한/영 혼합 메타데이터 처리에 적합
 *
 * Spring AI 의 EmbeddingModel 추상화 덕에 호출 코드는 OpenAI/Ollama 동일.
 * 모델 교체 시 application.yml + 의존성만 바꾸면 됨.
 *
 * Resilience4j:
 *  - retry: Ollama 데몬이 모델 로딩 중일 때 일시적 오류 대응
 *  - circuit breaker: 데몬 다운 시 fail-fast
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingClient {

    private final EmbeddingModel embeddingModel;  // Spring AI Auto-configuration

    @Getter
    @Value("${spring.ai.ollama.embedding.options.model}")
    private String modelVersion;

    /**
     * 단일 텍스트 임베딩.
     */
    @Retry(name = "ollamaEmbedding")
    @CircuitBreaker(name = "ollamaEmbedding", fallbackMethod = "embedFallback")
    public float[] embed(String text) {
        log.debug("Calling Ollama embedding API, text length={}", text.length());
        return embeddingModel.embed(text);
    }

    /**
     * 배치 임베딩.
     *
     * 주의: Ollama 의 batch 처리는 GPU/RAM 메모리에 민감.
     *  - bge-m3 batch 는 보통 32~64 정도가 안정적
     *  - application.yml 에서 batch-size 를 50으로 제한
     *  - 큰 batch 는 호출자가 청크로 쪼개서 보내야 함
     */
    @Retry(name = "ollamaEmbedding")
    @CircuitBreaker(name = "ollamaEmbedding", fallbackMethod = "embedBatchFallback")
    public List<float[]> embedBatch(List<String> texts) {
        log.info("Calling Ollama batch embedding, count={}", texts.size());

        EmbeddingResponse response = embeddingModel.embedForResponse(texts);

        return response.getResults().stream()
                .map(Embedding::getOutput)
                .toList();
    }

    // ─── Fallback ───
    @SuppressWarnings("unused")
    private float[] embedFallback(String text, Throwable t) {
        log.error("Ollama embedding failed permanently for text length={}", text.length(), t);
        throw new EmbeddingException(
                "Embedding service unavailable. " +
                        "Check: 1) `ollama serve` is running 2) `ollama pull bge-m3` was executed.", t);
    }

    @SuppressWarnings("unused")
    private List<float[]> embedBatchFallback(List<String> texts, Throwable t) {
        log.error("Batch embedding failed for {} texts", texts.size(), t);
        throw new EmbeddingException("Embedding service unavailable", t);
    }

    public static class EmbeddingException extends RuntimeException {
        public EmbeddingException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}
