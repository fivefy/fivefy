package com.fivefy.ai.service;

import com.fivefy.ai.domain.TrackEmbedding;
import com.fivefy.ai.dto.etc.TrackForEmbedding;
import com.fivefy.ai.enums.EmbeddingErrorCode;
import com.fivefy.ai.repository.TrackEmbeddingRepository;
import com.fivefy.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackEmbeddingPersistService {

    private final TrackEmbeddingRepository embeddingRepository;

    @Transactional(transactionManager = "vectorTransactionManager")
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

    @Transactional(transactionManager = "vectorTransactionManager")
    public void persistBatch(List<TrackForEmbedding> tracks,
                             List<String> texts,
                             List<String> hashes,
                             List<float[]> vectors,
                             String modelVersion) {
        if (tracks.size() != vectors.size() || tracks.size() != texts.size() || tracks.size() != hashes.size()) {
            log.error("배치 저장 데이터 크기 불일치: tracks={}, texts={}, hashes={}, vectors={}",
                    tracks.size(), texts.size(), hashes.size(), vectors.size());

            throw new BusinessException(EmbeddingErrorCode.ERR_EMBEDDING_BATCH_SIZE_MISMATCH);
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
