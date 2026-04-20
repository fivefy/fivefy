package com.fivefy.domain.track.dto.response;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.track.entity.TrackApplication;
import com.fivefy.domain.track.enums.TrackType;

import java.time.LocalDateTime;

/**
 * 트랙 등록 신청 상세 응답 DTO
 */
public record TrackApplicationDetailResponse(
        Long applicationId,
        Long requesterUserId,
        TrackType trackType,
        Long artistId,
        Long albumId,
        Long trackNumber,
        String title,
        String lyrics,
        String genre,
        String audioUrl,
        Long durationSec,
        String featuredArtistText,
        Integer publishDelayDays,
        ApplicationStatus status,
        Long reviewedByAdminId,
        LocalDateTime reviewedAt,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TrackApplicationDetailResponse from(TrackApplication application) {
        return new TrackApplicationDetailResponse(
                application.getId(),
                application.getRequesterUserId(),
                application.getTrackType(),
                application.getArtistId(),
                application.getAlbumId(),
                application.getTrackNumber(),
                application.getTitle(),
                application.getLyrics(),
                application.getGenre(),
                application.getAudioUrl(),
                application.getDurationSec(),
                application.getFeaturedArtistText(),
                application.getPublishDelayDays(),
                application.getStatus(),
                application.getReviewedByAdminId(),
                application.getReviewedAt(),
                application.getRejectionReason(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}