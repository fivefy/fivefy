package com.fivefy.domain.track.service;

import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.album.enums.AlbumStatus;
import com.fivefy.domain.album.repository.AlbumRepository;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.artist.enums.ArtistStatus;
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
import com.fivefy.domain.user.enums.UserRole;
import com.fivefy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        // 댓글 작성 유저 존재 확인
        findUser(userId);

        // 댓글 접근 가능한 트랙 조회
        findAccessibleCommentTrack(trackId);

        TrackComment savedComment = trackCommentRepository.save(
                TrackComment.create(userId, trackId, request.content())
        );

        return TrackCommentResponse.from(savedComment);
    }

    /**
     * 트랙 댓글 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<TrackCommentResponse> getTrackComments(Long trackId, Pageable pageable) {
        // 댓글 접근 가능한 트랙 조회
        findAccessibleCommentTrack(trackId);

        Page<TrackComment> page = trackCommentRepository.getTrackComments(trackId, pageable);

        return PageResponse.from(
                page.map(TrackCommentResponse::from)
        );
    }

    /**
     * 트랙 댓글 수정
     */
    @Transactional
    public TrackCommentResponse updateTrackComment(
            Long userId,
            Long trackId,
            Long commentId,
            TrackCommentCreateRequest request
    ) {
        // 유저 조회
        User user = findUser(userId);

        // 트랙 접근 가능 여부 검증
        findAccessibleCommentTrack(trackId);

        // 트랙 댓글 조회
        TrackComment comment = findTrackComment(trackId, commentId);

        // 작성자 검증
        if (!comment.isWrittenBy(user.getId())) {
            throw new BusinessException(TrackCommentErrorCode.ERR_FORBIDDEN_TRACK_COMMENT_UPDATE);
        }

        // 댓글 내용 수정
        comment.updateContent(request.content());

        return TrackCommentResponse.from(comment);
    }

    /**
     * 트랙 댓글 삭제
     */
    @Transactional
    public void deleteTrackComment(
            Long userId,
            Long trackId,
            Long commentId
    ) {
        // 댓글 접근 가능한 트랙 조회
        findAccessibleCommentTrack(trackId);

        // 유저 조회
        User user = findUser(userId);

        // 트랙 댓글 조회
        TrackComment comment = findTrackComment(trackId, commentId);

        // 작성자 또는 관리자만 삭제 가능
        validateCommentDeleteAccess(user, comment);

        // 댓글 삭제
        comment.softDelete();
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

    // 공개 가능한 트랙 조회
    private Track findPublishedTrack(Long trackId) {
        Track track = findTrack(trackId);

        if (track.getDeletedAt() != null || track.getStatus() != TrackStatus.PUBLISHED) {
            throw new BusinessException(TrackErrorCode.ERR_TRACK_NOT_FOUND);
        }

        return track;
    }

    // 공개 조회 가능한 앨범 조회
    private void findVisibleAlbum(Long albumId) {
        albumRepository.findById(albumId)
                .filter(album -> album.getDeletedAt() == null
                        && album.getStatus() == AlbumStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(TrackErrorCode.ERR_TRACK_NOT_FOUND));
    }

    // 공개 조회 가능한 아티스트 조회
    private void findVisibleArtist(Long artistId) {
        artistRepository.findById(artistId)
                .filter(artist -> !artist.isDeleted()
                        && artist.getStatus() == ArtistStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(TrackErrorCode.ERR_TRACK_NOT_FOUND));
    }

    // 댓글 접근 가능한 트랙 조회
    private void findAccessibleCommentTrack(Long trackId) {
        Track track = findPublishedTrack(trackId);

        if (track.getTrackType() == TrackType.OFFICIAL_RELEASE) {
            validateOfficialTrackVisibility(track);
        }
    }

    // 트랙 댓글 조회
    private TrackComment findTrackComment(Long trackId, Long commentId) {
        TrackComment comment = trackCommentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(
                        TrackCommentErrorCode.ERR_TRACK_COMMENT_NOT_FOUND));

        validateCommentTrack(trackId, comment);

        return comment;
    }


    // =========================
    // 검증
    // =========================

    // 정식 발매 트랙 공개 가능 상태 검증
    private void validateOfficialTrackVisibility(Track track) {
        findVisibleAlbum(track.getAlbumId());
        findVisibleArtist(track.getArtistId());
    }

    // 댓글 삭제 권한 검증
    private void validateCommentDeleteAccess(User user, TrackComment comment) {
        boolean isWriter = comment.isWrittenBy(user.getId());
        boolean isAdmin = user.getRole() == UserRole.ADMIN;

        if (!isWriter && !isAdmin) {
            throw new BusinessException(
                    TrackCommentErrorCode.ERR_FORBIDDEN_TRACK_COMMENT_DELETE
            );
        }
    }

    // 댓글이 해당 트랙에 속하는지 검증
    private void validateCommentTrack(Long trackId, TrackComment comment) {
        if (!comment.getTrackId().equals(trackId)) {
            throw new BusinessException(TrackCommentErrorCode.ERR_TRACK_COMMENT_NOT_FOUND);
        }
    }
}