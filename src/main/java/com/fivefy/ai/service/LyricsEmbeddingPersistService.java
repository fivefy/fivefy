package com.fivefy.ai.service;

import com.fivefy.ai.domain.TrackLyricsEmbedding;
import com.fivefy.ai.repository.TrackLyricsEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LyricsEmbeddingPersistService {

    private final TrackLyricsEmbeddingRepository lyricsEmbeddingRepository;

    @Transactional(readOnly = true, transactionManager = "vectorTransactionManager")
    public String getExistingHash(Long trackId) {
        return lyricsEmbeddingRepository.findHashByTrackId(trackId);
    }

    @Transactional(transactionManager = "vectorTransactionManager")
    public void save(TrackLyricsEmbedding embedding) {
        lyricsEmbeddingRepository.upsert(embedding);
    }
}
