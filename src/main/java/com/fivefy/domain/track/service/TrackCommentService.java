package com.fivefy.domain.track.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.album.entity.Album;
import com.fivefy.domain.album.enums.AlbumErrorCode;
import com.fivefy.domain.album.enums.AlbumStatus;
import com.fivefy.domain.album.repository.AlbumRepository;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.artist.enums.ArtistErrorCode;
import com.fivefy.domain.artist.repository.ArtistRepository;
import com.fivefy.domain.track.dto.request.TrackCommentCreateRequest;
import com.fivefy.domain.track.dto.response.TrackCommentResponse;
import com.fivefy.domain.track.entity.Track;
import com.fivefy.domain.track.entity.TrackComment;
import com.fivefy.domain.track.enums.TrackCommentErrorCode;
import com.fivefy.domain.track.enums.TrackErrorCode;
import com.fivefy.domain.track.enums.TrackStatus;
import com.fivefy.domain.track.enums.TrackType;
import com.fivefy.domain.track.repository.TrackCommentRepository;
import com.fivefy.domain.track.repository.TrackRepository;
import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.enums.UserErrorCode;
import com.fivefy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 트랙 댓글 도메인 서비스
 */
@Service
@RequiredArgsConstructor
public class TrackCommentService {

    private final TrackCommentRepository trackCommentRepository;
    private final TrackRepository trackRepository;
    private final UserRepository userRepository;
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;

    /**
     * 트랙 댓글 작성
     */
    @Transactional
    public TrackCommentResponse createTrackComment(
            Long userId,
            Long trackId,
            TrackCommentCreateRequest request
    ) {
        findUser(userId);

        Track track = findTrack(trackId);

        // 현재 조회 가능한 트랙에만 댓글 작성 허용
        validateTrackCommentWritable(track);

        TrackComment savedComment = trackCommentRepository.save(
                TrackComment.create(userId, trackId, request.content())
        );

        return TrackCommentResponse.from(savedComment);
    }

    // =========================
    // 조회
    // =========================

    // 유저 조회
    private User findUser(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.ERR_USER_NOT_FOUND));
    }

    // 트랙 조회
    private Track findTrack(Long trackId) {
        return trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(TrackErrorCode.ERR_TRACK_NOT_FOUND));
    }

    // 앨범 조회
    private Album findAlbum(Long albumId) {
        return albumRepository.findById(albumId)
                .orElseThrow(() -> new BusinessException(AlbumErrorCode.ERR_ALBUM_NOT_FOUND));
    }

    // 아티스트 조회
    private Artist findArtist(Long artistId) {
        return artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ArtistErrorCode.ERR_ARTIST_NOT_FOUND));
    }

    // =========================
    // 검증
    // =========================

    // 댓글 작성 가능한 트랙인지 검증
    private void validateTrackCommentWritable(Track track) {
        if (track.getDeletedAt() != null || track.getStatus() != TrackStatus.PUBLISHED) {
            throw new BusinessException(TrackCommentErrorCode.ERR_TRACK_COMMENT_NOT_WRITABLE);
        }

        // 정식 발매 트랙은 연관 앨범/아티스트 공개 가능 상태까지 확인
        if (track.getTrackType() == TrackType.OFFICIAL_RELEASE) {
            validateOfficialReleaseTrackCommentWritable(track);
        }
    }

    // 정식 발매 트랙 댓글 작성 가능 상태 검증
    private void validateOfficialReleaseTrackCommentWritable(Track track) {
        Album album = findAlbum(track.getAlbumId());

        if (album.getDeletedAt() != null || album.getStatus() != AlbumStatus.PUBLISHED) {
            throw new BusinessException(TrackCommentErrorCode.ERR_TRACK_COMMENT_NOT_WRITABLE);
        }

        Artist artist = findArtist(track.getArtistId());

        if (artist.isDeleted()) {
            throw new BusinessException(TrackCommentErrorCode.ERR_TRACK_COMMENT_NOT_WRITABLE);
        }
    }
}