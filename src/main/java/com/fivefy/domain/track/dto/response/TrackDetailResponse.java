package com.fivefy.domain.track.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fivefy.domain.track.entity.Track;
import com.fivefy.domain.track.enums.TrackType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 트랙 상세 조회 응답 DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TrackDetailResponse(
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
        Long playCount,
        LocalDateTime publishedAt,
        List<TrackCommentResponse> comments
) {
    public static TrackDetailResponse of(
            Track track,
            String artistName,
            String albumTitle,
            List<TrackCommentResponse> comments
    ) {
        return new TrackDetailResponse(
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
                track.getPlayCount(),
                track.getPublishedAt(),
                comments
        );
    }
}