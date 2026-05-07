package com.fivefy.ai.service;

import com.fivefy.ai.dto.etc.BatchResult;
import com.fivefy.ai.dto.etc.TrackForEmbedding;
import com.fivefy.ai.repository.TrackEmbeddingRepository;
import com.fivefy.common.enums.AlgorithmErrorCode;
import com.fivefy.common.exception.BusinessException;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackEmbeddingService {

    private final EmbeddingClient embeddingClient;
    private final TrackEmbeddingRepository embeddingRepository;
    private final TrackEmbeddingPersistService persistService;

    public boolean embedSingle(TrackForEmbedding track) {
        String text = track.toEmbeddingText();
        String hash = sha256(text);

        // 1) hash 조회 (짧은 read)
        String existingHash = embeddingRepository.findHashByTrackId(track.trackId());
        if (hash.equals(existingHash)) {
            log.debug("Skip trackId={}, 콘텐츠 변동 없음", track.trackId());
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

    public BatchResult embedBatch(List<TrackForEmbedding> tracks) {
        if (tracks.isEmpty()) {
            return new BatchResult(0, 0, 0);
        }

        // 1) 변경된 트랙만 추리기 (hash 일괄 조회)
        ChangedSet changed = filterChanged(tracks);

        if (changed.isEmpty()) {
            log.info("임베딩할 트랙 없음 (모두 최신 상태), totalChecked={}", tracks.size());
            return new BatchResult(0, tracks.size(), 0);
        }

        int skipped = tracks.size() - changed.tracks.size();

        // 2) Ollama batch 호출 (트랜잭션 밖)
        List<float[]> vectors;
        try {
            vectors = embeddingClient.embedBatch(changed.texts);
        } catch (Exception e) {
            log.error("배치 임베딩 API 요청 중 오류 발생, size={}", changed.tracks.size(), e);
            return new BatchResult(0, skipped, changed.tracks.size());
        }

        if (vectors.size() != changed.tracks.size()) {
            log.error("임베딩 벡터 개수 불일치: expected={}, actual={}",
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
            log.error("임베딩 데이터 배치 저장 중 오류 발생, size={}", changed.tracks.size(), e);
            return new BatchResult(0, skipped, changed.tracks.size());
        }

        log.info("임베딩 배치 작업 완료: processed={}, skipped={}", changed.tracks.size(), skipped);
        return new BatchResult(changed.tracks.size(), skipped, 0);
    }

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
            throw new BusinessException(AlgorithmErrorCode.ERR_ALGORITHM_NOT_FOUND);
        }
    }

    private record ChangedSet(
            List<TrackForEmbedding> tracks,
            List<String> texts,
            List<String> hashes
    ) {
        boolean isEmpty() { return tracks.isEmpty(); }
    }
}
