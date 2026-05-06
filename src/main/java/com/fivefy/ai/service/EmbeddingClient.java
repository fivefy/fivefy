package com.fivefy.ai.service;

import com.fivefy.ai.enums.EmbeddingErrorCode;
import com.fivefy.common.exception.BusinessException;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingClient {

    private final EmbeddingModel embeddingModel;

    @Getter
    @Value("${spring.ai.ollama.embedding.options.model}")
    private String modelVersion;

    @Retry(name = "ollamaEmbedding")
    @CircuitBreaker(name = "ollamaEmbedding", fallbackMethod = "embedFallback")
    public float[] embed(String text) {
        log.debug("Ollama 임베딩 API 요청, text length={}", text.length());
        return embeddingModel.embed(text);
    }

    @Retry(name = "ollamaEmbedding")
    @CircuitBreaker(name = "ollamaEmbedding", fallbackMethod = "embedBatchFallback")
    public List<float[]> embedBatch(List<String> texts) {
        log.info("Ollama 배치 임베딩 요청, count={}", texts.size());

        EmbeddingResponse response = embeddingModel.embedForResponse(texts);

        return response.getResults().stream()
                .map(Embedding::getOutput)
                .toList();
    }

    // ─── Fallback ───
    @SuppressWarnings("unused")
    private float[] embedFallback(String text, Throwable t) {
        log.error("Ollama 임베딩 최종 실패: text length={}", text.length(), t);
        throw new BusinessException(EmbeddingErrorCode.ERR_EMBEDDING_SERVICE_UNAVAILABLE);
    }

    @SuppressWarnings("unused")
    private List<float[]> embedBatchFallback(List<String> texts, Throwable t) {
        log.error("Ollama 배치 임베딩 실패 for {} texts", texts.size(), t);
        throw new BusinessException(EmbeddingErrorCode.ERR_EMBEDDING_SERVICE_UNAVAILABLE);
    }
}
