package com.fivefy.ai.service;

import com.fivefy.ai.dto.TrackForEmbedding;
import com.fivefy.ai.repository.TrackEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * 트랙 임베딩 오케스트레이션.
 *
 * 책임:
 *  - 변경 감지 (hash 비교)
 *  - 외부 API(Ollama) 호출
 *  - 영속화는 EmbeddingPersistService 에 위임
 *
 * 이 클래스에는 @Transactional 이 없다 — 의도된 것:
 *  - 외부 API 호출이 트랜잭션 안에 들어가면 DB 커넥션을 수 초간 점유 → 풀 고갈
 *  - 영속화 책임을 별도 빈에 위임해서 self-invocation 함정 회피
 *  - 각 단계는 자체적으로 짧은 트랜잭션을 가짐 (READ / WRITE 분리)
 *
 * 핵심 최적화:
 *  1) source_hash 로 변경 감지 → 동일 텍스트면 Ollama 호출 skip
 *  2) hash 조회를 IN절 한 번으로 (N+1 제거)
 *  3) batch 단위 호출
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackEmbeddingService {

    private final EmbeddingClient embeddingClient;
    private final TrackEmbeddingRepository embeddingRepository;
    private final EmbeddingPersistService persistService;

    /**
     * 트랙 한 건 임베딩.
     * 이미 같은 내용으로 임베딩 되어있으면 skip.
     */
    public boolean embedSingle(TrackForEmbedding track) {
        String text = track.toEmbeddingText();
        String hash = sha256(text);

        // 1) hash 조회 (짧은 read)
        String existingHash = embeddingRepository.findHashByTrackId(track.trackId());
        if (hash.equals(existingHash)) {
            log.debug("Skip trackId={}, content unchanged", track.trackId());
            return false;
        }

        // 2) 외부 API 호출 (트랜잭션 밖)
        float[] vector = embeddingClient.embed(text);

        // 3) UPSERT (별도 빈 → @Transactional 적용 보장)
        persistService.persistSingle(
                track.trackId(),
                vector,
                text,
                hash,
                embeddingClient.getModelVersion()
        );
        return true;
    }

    /**
     * 배치 임베딩.
     *
     * 흐름:
     *   filterChanged (read)  →  Ollama 호출 (no tx)  →  persistBatch (write tx)
     *
     * Ollama 호출 실패 시 청크 전체를 failed 로 카운트하고 다음 청크로 진행.
     * persistBatch 가 던지는 예외도 잡아서 청크 단위 격리.
     */
    public BatchResult embedBatch(List<TrackForEmbedding> tracks) {
        if (tracks.isEmpty()) {
            return new BatchResult(0, 0, 0);
        }

        // 1) 변경된 트랙만 추리기 (hash 일괄 조회)
        ChangedSet changed = filterChanged(tracks);

        if (changed.isEmpty()) {
            log.info("No tracks to embed (all up-to-date), totalChecked={}", tracks.size());
            return new BatchResult(0, tracks.size(), 0);
        }

        int skipped = tracks.size() - changed.tracks.size();

        // 2) Ollama batch 호출 (트랜잭션 밖)
        List<float[]> vectors;
        try {
            vectors = embeddingClient.embedBatch(changed.texts);
        } catch (Exception e) {
            log.error("Batch embedding API call failed, size={}", changed.tracks.size(), e);
            return new BatchResult(0, skipped, changed.tracks.size());
        }

        if (vectors.size() != changed.tracks.size()) {
            log.error("Vector count mismatch: expected={}, actual={}",
                    changed.tracks.size(), vectors.size());
            return new BatchResult(0, skipped, changed.tracks.size());
        }

        // 3) UPSERT (트랜잭션 적용 — 청크 atomicity)
        try {
            persistService.persistBatch(
                    changed.tracks,
                    changed.texts,
                    changed.hashes,
                    vectors,
                    embeddingClient.getModelVersion()
            );
        } catch (Exception e) {
            log.error("Batch persist failed, size={}", changed.tracks.size(), e);
            return new BatchResult(0, skipped, changed.tracks.size());
        }

        log.info("Batch embedding done: processed={}, skipped={}", changed.tracks.size(), skipped);
        return new BatchResult(changed.tracks.size(), skipped, 0);
    }

    /**
     * hash를 IN절로 한 번에 조회해서 변경된 트랙만 골라냄.
     * 청크 100건이면 SELECT는 1회 (N+1 제거).
     */
    private ChangedSet filterChanged(List<TrackForEmbedding> tracks) {
        List<Long> trackIds = tracks.stream().map(TrackForEmbedding::trackId).toList();
        Map<Long, String> existingHashes = embeddingRepository.findHashesByTrackIds(trackIds);

        List<TrackForEmbedding> changedTracks = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        List<String> hashes = new ArrayList<>();

        for (TrackForEmbedding track : tracks) {
            String text = track.toEmbeddingText();
            String hash = sha256(text);

            if (hash.equals(existingHashes.get(track.trackId()))) {
                continue;
            }

            changedTracks.add(track);
            texts.add(text);
            hashes.add(hash);
        }

        return new ChangedSet(changedTracks, texts, hashes);
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** filterChanged 결과 묶음. */
    private record ChangedSet(
            List<TrackForEmbedding> tracks,
            List<String> texts,
            List<String> hashes
    ) {
        boolean isEmpty() { return tracks.isEmpty(); }
    }

    public record BatchResult(int processed, int skipped, int failed) {}
}
