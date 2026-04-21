package com.fivefy.domain.track.service;

import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.album.entity.Album;
import com.fivefy.domain.album.enums.AlbumErrorCode;
import com.fivefy.domain.album.enums.AlbumStatus;
import com.fivefy.domain.album.repository.AlbumRepository;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.artist.enums.ArtistErrorCode;
import com.fivefy.domain.artist.enums.ArtistStatus;
import com.fivefy.domain.artist.repository.ArtistRepository;
import com.fivefy.domain.track.dto.request.FreeTrackApplicationCreateRequest;
import com.fivefy.domain.track.dto.request.OfficialTrackApplicationCreateRequest;
import com.fivefy.domain.track.dto.response.*;
import com.fivefy.domain.track.entity.Track;
import com.fivefy.domain.track.entity.TrackApplication;
import com.fivefy.domain.track.enums.TrackApplicationErrorCode;
import com.fivefy.domain.track.enums.TrackErrorCode;
import com.fivefy.domain.track.enums.TrackStatus;
import com.fivefy.domain.track.enums.TrackType;
import com.fivefy.domain.track.repository.*;
import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.enums.UserErrorCode;
import com.fivefy.domain.user.enums.UserRole;
import com.fivefy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 트랙 도메인 서비스
 */
@Service
@RequiredArgsConstructor
public class TrackService {

    private final TrackApplicationRepository trackApplicationRepository;
    private final UserRepository userRepository;
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final TrackRepository trackRepository;
    private final TrackCommentRepository trackCommentRepository;

    /**
     * 자유 창작 트랙 등록 신청
     */
    @Transactional
    public TrackApplicationResponse createFreeTrackApplication(
            Long userId,
            FreeTrackApplicationCreateRequest request
    ) {
        // 신청 유저 존재 확인
        findUser(userId);

        // 자유 창작 PENDING 중복 신청 검증
        validateDuplicateFreeCreationApplication(userId, request.title(), request.audioUrl());

        // 등록 신청 생성 및 저장
        TrackApplication savedApplication = trackApplicationRepository.save(
                TrackApplication.create(
                        userId,
                        TrackType.FREE_CREATION,
                        null,
                        null,
                        null,
                        request.title(),
                        request.lyrics(),
                        request.genre(),
                        request.audioUrl(),
                        request.durationSec(),
                        null,
                        null
                )
        );

        return TrackApplicationResponse.from(savedApplication);
    }

    /**
     * 정식 발매 트랙 등록 신청
     */
    @Transactional
    public TrackApplicationResponse createOfficialTrackApplication(
            Long userId,
            OfficialTrackApplicationCreateRequest request
    ) {
        // 신청 유저 존재 확인
        findUser(userId);

        // 아티스트 조회 및 삭제 여부 검증
        Artist artist = findNotDeletedArtist(request.artistId());

        // 소유자 검증
        validateArtistOwner(userId, artist);

        // 아티스트 상태 검증
        validateArtistActive(artist);

        // 앨범 조회 및 삭제 여부 검증
        Album album = findNotDeletedAlbum(request.albumId());

        // 앨범-아티스트 일치 검증
        validateAlbumArtistMatch(album, request.artistId());

        // 공개 예약 옵션 검증
        validatePublishDelayDays(request.publishDelayDays());

        // 정식 발매 중복 신청 검증
        validateDuplicateOfficialReleaseApplication(
                userId,
                request.artistId(),
                request.albumId(),
                request.trackNumber(),
                request.title()
        );

        // 등록 신청 생성 및 저장
        TrackApplication savedApplication = trackApplicationRepository.save(
                TrackApplication.create(
                        userId,
                        TrackType.OFFICIAL_RELEASE,
                        request.artistId(),
                        request.albumId(),
                        request.trackNumber(),
                        request.title(),
                        request.lyrics(),
                        request.genre(),
                        request.audioUrl(),
                        request.durationSec(),
                        request.featuredArtistText(),
                        request.publishDelayDays()
                )
        );

        return TrackApplicationResponse.from(savedApplication);
    }

    /**
     * 내 트랙 등록 신청 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TrackApplicationResponse> getMyTrackApplications(Long userId) {
        findUser(userId);

        return trackApplicationRepository.searchMyTrackApplications(userId)
                .stream()
                .map(TrackApplicationResponse::from)
                .toList();
    }

    /**
     * 트랙 등록 신청 상세 조회
     */
    @Transactional(readOnly = true)
    public TrackApplicationDetailResponse getTrackApplication(Long userId, Long applicationId) {
        TrackApplication application = findTrackApplication(applicationId);
        User user = findUser(userId);

        // 신청자 또는 관리자만 조회 가능
        validateTrackApplicationDetailAccess(userId, user, application);

        return TrackApplicationDetailResponse.from(application);
    }

    /**
     * 트랙 등록 신청 목록 조회 (관리자)
     */
    @Transactional(readOnly = true)
    public PageResponse<TrackApplicationListResponse> getTrackApplications(
            ApplicationStatus status,
            Pageable pageable
    ) {
        Page<TrackApplication> page =
                trackApplicationRepository.searchTrackApplications(status, pageable);

        return PageResponse.from(
                page.map(TrackApplicationListResponse::from)
        );
    }

    /**
     * 트랙 등록 신청 승인 (관리자)
     */
    @Transactional
    public TrackApplicationApproveResponse approveTrackApplication(Long adminId, Long applicationId) {
        TrackApplication application = findTrackApplication(applicationId);

        // OFFICIAL_RELEASE는 승인 시점에도 연관 리소스 유효성 재확인
        if (application.getTrackType() == TrackType.OFFICIAL_RELEASE) {
            Artist artist = findNotDeletedArtist(application.getArtistId());
            validateArtistActive(artist);

            Album album = findNotDeletedAlbum(application.getAlbumId());
            validateAlbumArtistMatch(album, application.getArtistId());
        }

        // 이미 처리된 신청은 엔티티에서 상태 전이 불가 처리
        application.approve(adminId);

        // 승인된 신청 기반으로 트랙 생성
        Track savedTrack = trackRepository.save(createTrack(application));

        return TrackApplicationApproveResponse.from(application, savedTrack.getId());
    }

    /**
     * 트랙 등록 신청 거절 (관리자)
     */
    @Transactional
    public TrackApplicationRejectResponse rejectTrackApplication(
            Long adminId,
            Long applicationId,
            String rejectionReason
    ) {
        TrackApplication application = findTrackApplication(applicationId);

        // 상태 전이는 엔티티에 위임
        application.reject(adminId, rejectionReason);

        return TrackApplicationRejectResponse.from(application);
    }

    /**
     * 트랙 상세 조회
     */
    @Transactional(readOnly = true)
    public TrackDetailResponse getTrack(Long trackId) {
        Track track = findPublishedTrack(trackId);

        // 정식 발매 트랙은 연관 앨범/아티스트 공개 가능 상태까지 검증
        if (track.getTrackType() == TrackType.OFFICIAL_RELEASE) {
            validateOfficialTrackVisibility(track);
        }

        TrackDetailProjection projection = trackRepository.findTrackDetailById(trackId);

        String artistName = projection == null ? null : projection.artistName();
        String albumTitle = projection == null ? null : projection.albumTitle();

        List<TrackCommentResponse> comments = trackCommentRepository
                .getRecentTrackComments(trackId, 5)
                .stream()
                .map(TrackCommentResponse::from)
                .toList();

        return TrackDetailResponse.of(track, artistName, albumTitle, comments);
    }

    /**
     * 공개 트랙 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<PublicTrackListResponse> getPublicTracks(Pageable pageable) {

        // 공개 트랙 목록 조회 (Querydsl)
        Page<PublicTrackListProjection> page =
                trackRepository.searchPublicTracks(pageable);

        // Projection → 응답 DTO 변환
        return PageResponse.from(
                page.map(projection -> PublicTrackListResponse.of(
                        projection.trackId(),
                        projection.trackType(),
                        projection.title(),
                        projection.artistId(),
                        projection.artistName(),
                        projection.albumId(),
                        projection.albumTitle(),
                        projection.durationSec(),
                        projection.playCount(),
                        projection.publishedAt()
                ))
        );
    }

    /**
     * 아티스트별 자유 창작 트랙 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<ArtistFreeCreationTrackResponse> getArtistFreeCreations(
            Long artistId,
            Pageable pageable
    ) {

        // 삭제되지 않은 아티스트 확인
        Artist artist = findNotDeletedArtist(artistId);

        // 아티스트 소유 유저의 공개 자유 창작 트랙 목록 조회
        Page<Track> page =
                trackRepository.searchArtistFreeCreations(artist.getOwnerUserId(), pageable);

        // 엔티티 → 응답 DTO 변환
        return PageResponse.from(
                page.map(ArtistFreeCreationTrackResponse::from)
        );
    }

    // =========================
    // 조회
    // =========================

    // 유저 조회
    private User findUser(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.ERR_USER_NOT_FOUND));
    }

    // 아티스트 조회
    private Artist findArtist(Long artistId) {
        return artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ArtistErrorCode.ERR_ARTIST_NOT_FOUND));
    }

    // 삭제되지 않은 아티스트 조회
    private Artist findNotDeletedArtist(Long artistId) {
        Artist artist = findArtist(artistId);

        if (artist.isDeleted()) {
            throw new BusinessException(ArtistErrorCode.ERR_ARTIST_NOT_FOUND);
        }

        return artist;
    }

    // 앨범 조회
    private Album findAlbum(Long albumId) {
        return albumRepository.findById(albumId)
                .orElseThrow(() -> new BusinessException(AlbumErrorCode.ERR_ALBUM_NOT_FOUND));
    }

    // 삭제되지 않은 앨범 조회
    private Album findNotDeletedAlbum(Long albumId) {
        Album album = findAlbum(albumId);

        if (album.getDeletedAt() != null) {
            throw new BusinessException(AlbumErrorCode.ERR_ALBUM_NOT_FOUND);
        }

        return album;
    }

    // 트랙 등록 신청 조회
    private TrackApplication findTrackApplication(Long applicationId) {
        return trackApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(
                        TrackApplicationErrorCode.ERR_TRACK_APPLICATION_NOT_FOUND));
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

    // =========================
    // 검증
    // =========================

    // 아티스트 소유자 검증
    private void validateArtistOwner(Long userId, Artist artist) {
        if (!artist.isOwnedBy(userId)) {
            throw new BusinessException(ArtistErrorCode.ERR_FORBIDDEN_ARTIST_ACCESS);
        }
    }

    // 아티스트 상태 검증
    private void validateArtistActive(Artist artist) {
        if (artist.getStatus() != ArtistStatus.ACTIVE) {
            throw new BusinessException(
                    TrackApplicationErrorCode.ERR_INACTIVE_ARTIST_CANNOT_REQUEST_OFFICIAL_RELEASE
            );
        }
    }

    // 앨범-아티스트 일치 검증
    private void validateAlbumArtistMatch(Album album, Long artistId) {
        if (!album.getArtistId().equals(artistId)) {
            throw new BusinessException(TrackApplicationErrorCode.ERR_ALBUM_ARTIST_MISMATCH);
        }
    }

    // 공개 예약 옵션 검증
    private void validatePublishDelayDays(Integer publishDelayDays) {
        if (publishDelayDays == null || publishDelayDays < 0 || publishDelayDays > 7) {
            throw new BusinessException(
                    TrackApplicationErrorCode.ERR_INVALID_PUBLISH_DELAY_DAYS
            );
        }
    }

    // 자유 창작 중복 신청 검증
    private void validateDuplicateFreeCreationApplication(
            Long requesterUserId,
            String title,
            String audioUrl
    ) {
        if (trackApplicationRepository.existsPendingFreeCreationApplication(
                requesterUserId,
                title,
                audioUrl
        )) {
            throw new BusinessException(
                    TrackApplicationErrorCode.ERR_TRACK_APPLICATION_ALREADY_EXISTS
            );
        }
    }

    // 정식 발매 중복 신청 검증
    private void validateDuplicateOfficialReleaseApplication(
            Long requesterUserId,
            Long artistId,
            Long albumId,
            Long trackNumber,
            String title
    ) {
        if (trackApplicationRepository.existsPendingOfficialReleaseApplication(
                requesterUserId,
                artistId,
                albumId,
                trackNumber,
                title
        )) {
            throw new BusinessException(
                    TrackApplicationErrorCode.ERR_TRACK_APPLICATION_ALREADY_EXISTS
            );
        }
    }

    // 상세 조회 권한 검증
    private void validateTrackApplicationDetailAccess(
            Long userId,
            User user,
            TrackApplication application
    ) {
        boolean isRequester = application.getRequesterUserId().equals(userId);
        boolean isAdmin = user.getRole() == UserRole.ADMIN;

        if (!isRequester && !isAdmin) {
            throw new BusinessException(
                    TrackApplicationErrorCode.ERR_TRACK_APPLICATION_DETAIL_FORBIDDEN
            );
        }
    }

    // 정식 발매 트랙 공개 가능 상태 검증
    private void validateOfficialTrackVisibility(Track track) {
        Album album = findAlbum(track.getAlbumId());

        if (album.getDeletedAt() != null || album.getStatus() != AlbumStatus.PUBLISHED) {
            throw new BusinessException(TrackErrorCode.ERR_TRACK_NOT_FOUND);
        }

        Artist artist = findArtist(track.getArtistId());

        if (artist.isDeleted()) {
            throw new BusinessException(TrackErrorCode.ERR_TRACK_NOT_FOUND);
        }
    }

    // =========================
    // 생성 / 후처리
    // =========================

    // 승인된 신청 기반 트랙 생성
    private Track createTrack(TrackApplication application) {
        if (application.getTrackType() == TrackType.FREE_CREATION) {
            return Track.createFreeCreation(
                    application.getRequesterUserId(),
                    application.getTitle(),
                    application.getLyrics(),
                    application.getGenre(),
                    application.getAudioUrl(),
                    application.getDurationSec()
            );
        }

        LocalDateTime scheduledPublishAt =
                calculateScheduledPublishAt(application.getPublishDelayDays());

        Track track = Track.createOfficialRelease(
                application.getRequesterUserId(),
                application.getArtistId(),
                application.getAlbumId(),
                application.getTrackNumber(),
                application.getTitle(),
                application.getLyrics(),
                application.getGenre(),
                application.getAudioUrl(),
                application.getDurationSec(),
                application.getFeaturedArtistText(),
                scheduledPublishAt
        );

        // 즉시 공개
        if (application.getPublishDelayDays() == 0) {
            track.publish();
        }

        return track;
    }

    // 공개 예약 시각 계산
    private LocalDateTime calculateScheduledPublishAt(Integer publishDelayDays) {
        if (publishDelayDays == null || publishDelayDays == 0) {
            return null;
        }

        return LocalDateTime.now().plusDays(publishDelayDays);
    }
}