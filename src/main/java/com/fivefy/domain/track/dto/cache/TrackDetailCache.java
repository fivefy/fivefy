package com.fivefy.domain.track.dto.cache;

import com.fivefy.domain.track.entity.Track;
import com.fivefy.domain.track.enums.TrackType;

import java.time.LocalDateTime;

/**
 * 트랙 상세 캐시 DTO
 */
public record TrackDetailCache(
        Long trackId,
        TrackType trackType,
        Long artistId,
        String artistName,
        Long albumId,
        String albumTitle,
        Long trackNumber,
        String title,
        String lyrics,
        String genre,
        String audioUrl,
        Long durationSec,
        String featuredArtistText,
        LocalDateTime publishedAt
) {

    /**
     * 트랙 상세 캐시 생성
     */
    public static TrackDetailCache of(
            Track track,
            String artistName,
            String albumTitle
    ) {
        return new TrackDetailCache(
                track.getId(),
                track.getTrackType(),
                track.getArtistId(),
                artistName,
                track.getAlbumId(),
                albumTitle,
                track.getTrackNumber(),
                track.getTitle(),
                track.getLyrics(),
                track.getGenre(),
                track.getAudioUrl(),
                track.getDurationSec(),
                track.getFeaturedArtistText(),
                track.getPublishedAt()
        );
    }
}