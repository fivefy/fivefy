package com.fivefy.ai.service;

import com.fivefy.ai.domain.TrackEmbedding;
import com.fivefy.ai.dto.TrackForEmbedding;
import com.fivefy.ai.repository.TrackEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 트랙 임베딩 영속화 전담 빈.
 *
 * 왜 별도 빈으로 분리했나:
 *  Spring AOP 프록시는 같은 클래스 내부 호출(self-invocation)에는 트랜잭션을 적용하지 않음.
 *  TrackEmbeddingService 안에서 this.persistBatch(...) 로 호출하면 @Transactional이 무시됨.
 *  분리하면 프록시 경유가 보장되어 트랜잭션이 실제로 걸린다.
 *
 * 책임:
 *  - DB persist 만 담당 (외부 API 호출, hash 비교 X)
 *  - 청크 단위 atomicity 보장 (batchUpdate 중간 실패 시 전체 롤백)
 *
 * 트랜잭션 경계 원칙:
 *  - 외부 API(Ollama) 호출은 절대 이 빈에 들어오면 안 됨
 *  - 짧고 빠른 DB 작업만 → 커넥션 점유 시간 최소화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingPersistService {

    private final TrackEmbeddingRepository embeddingRepository;

    /**
     * 단건 UPSERT.
     */
    @Transactional("vectorTransactionManager")
    public void persistSingle(Long trackId,
                              float[] vector,
                              String text,
                              String hash,
                              String modelVersion) {
        embeddingRepository.upsert(TrackEmbedding.builder()
                .trackId(trackId)
                .embedding(vector)
                .sourceText(text)
                .sourceHash(hash)
                .embeddedAt(LocalDateTime.now())
                .modelVersion(modelVersion)
                .build());
    }

    /**
     * 배치 UPSERT — 청크 단위 atomicity.
     * 중간 row 하나가 제약 위반이면 전체 롤백되어 부분 커밋이 생기지 않음.
     */
    @Transactional("vectorTransactionManager")
    public void persistBatch(List<TrackForEmbedding> tracks,
                             List<String> texts,
                             List<String> hashes,
                             List<float[]> vectors,
                             String modelVersion) {
        if (tracks.size() != vectors.size() || tracks.size() != texts.size() || tracks.size() != hashes.size()) {
            throw new IllegalArgumentException(
                    "Size mismatch: tracks=%d, texts=%d, hashes=%d, vectors=%d"
                            .formatted(tracks.size(), texts.size(), hashes.size(), vectors.size()));
        }

        LocalDateTime now = LocalDateTime.now();
        List<TrackEmbedding> entities = new ArrayList<>(tracks.size());
        for (int i = 0; i < tracks.size(); i++) {
            entities.add(TrackEmbedding.builder()
                    .trackId(tracks.get(i).trackId())
                    .embedding(vectors.get(i))
                    .sourceText(texts.get(i))
                    .sourceHash(hashes.get(i))
                    .embeddedAt(now)
                    .modelVersion(modelVersion)
                    .build());
        }

        embeddingRepository.upsertBatch(entities);
    }
}
