package com.fivefy.domain.album.service;

import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.album.dto.request.AlbumApplicationCreateRequest;
import com.fivefy.domain.album.dto.response.*;
import com.fivefy.domain.album.entity.Album;
import com.fivefy.domain.album.entity.AlbumApplication;
import com.fivefy.domain.album.enums.AlbumApplicationErrorCode;
import com.fivefy.domain.album.enums.AlbumErrorCode;
import com.fivefy.domain.album.enums.AlbumStatus;
import com.fivefy.domain.album.repository.AlbumApplicationRepository;
import com.fivefy.domain.album.repository.AlbumRepository;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.artist.enums.ArtistErrorCode;
import com.fivefy.domain.artist.enums.ArtistStatus;
import com.fivefy.domain.artist.repository.ArtistRepository;
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

import java.time.LocalDateTime;
import java.util.List;

/**
 * 앨범 도메인 서비스
 */
@Service
@RequiredArgsConstructor
public class AlbumService {

    private final AlbumApplicationRepository albumApplicationRepository;
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final UserRepository userRepository;
    private final TrackRepository trackRepository;

    /**
     * 앨범 등록 신청 생성
     */
    @Transactional
    public AlbumApplicationResponse createAlbumApplication(
            Long userId,
            AlbumApplicationCreateRequest request
    ) {
        // 신청 유저 존재 확인
        findUser(userId);

        // 아티스트 조회 및 삭제 여부 검증
        Artist artist = findNotDeletedArtist(request.artistId());

        // 소유자 검증
        validateArtistOwner(userId, artist);

        // 아티스트 상태 검증
        validateArtistActive(artist);

        // 공개 예약 정책 검증
        validatePublishDelayDays(request.publishDelayDays());

        // 중복 신청 검증
        validateDuplicateAlbumApplication(userId, request.artistId(), request.title());

        // 등록 신청 생성 및 저장
        AlbumApplication savedApplication = albumApplicationRepository.save(
                AlbumApplication.create(
                        userId,
                        request.artistId(),
                        request.title(),
                        request.description(),
                        request.coverImageUrl(),
                        request.publishDelayDays()
                )
        );

        return AlbumApplicationResponse.from(savedApplication);
    }

    /**
     * 내 앨범 등록 신청 목록 조회
     */
    @Transactional(readOnly = true)
    public List<AlbumApplicationResponse> getMyAlbumApplications(Long userId) {
        findUser(userId);

        return albumApplicationRepository.searchMyAlbumApplications(userId)
                .stream()
                .map(AlbumApplicationResponse::from)
                .toList();
    }

    /**
     * 앨범 등록 신청 상세 조회
     */
    @Transactional(readOnly = true)
    public AlbumApplicationDetailResponse getAlbumApplication(Long userId, Long applicationId) {
        AlbumApplication application = findAlbumApplication(applicationId);
        User user = findUser(userId);

        // 신청자 또는 관리자만 조회 가능
        validateAlbumApplicationDetailAccess(userId, user, application);

        return AlbumApplicationDetailResponse.from(application);
    }

    /**
     * 앨범 등록 신청 목록 조회 (관리자)
     */
    @Transactional(readOnly = true)
    public PageResponse<AlbumApplicationListResponse> getAlbumApplications(
            ApplicationStatus status,
            Pageable pageable
    ) {
        Page<AlbumApplication> page =
                albumApplicationRepository.searchAlbumApplications(status, pageable);

        return PageResponse.from(
                page.map(AlbumApplicationListResponse::from)
        );
    }

    /**
     * 앨범 등록 신청 승인 (관리자)
     */
    @Transactional
    public AlbumApplicationApproveResponse approveAlbumApplication(Long adminId, Long applicationId) {
        AlbumApplication application = findAlbumApplicationForUpdate(applicationId);

        // 상태 전이는 엔티티에 위임
        application.approve(adminId);

        // 승인된 신청 기반 앨범 생성
        Album savedAlbum = albumRepository.save(createAlbum(application));

        return AlbumApplicationApproveResponse.from(application, savedAlbum.getId());
    }

    /**
     * 앨범 등록 신청 거절 (관리자)
     */
    @Transactional
    public AlbumApplicationRejectResponse rejectAlbumApplication(
            Long adminId,
            Long applicationId,
            String rejectionReason
    ) {
        AlbumApplication application = findAlbumApplicationForUpdate(applicationId);

        // 상태 전이는 엔티티에 위임
        application.reject(adminId, rejectionReason);

        return AlbumApplicationRejectResponse.from(application);
    }

    /**
     * 앨범 상세 조회
     */
    @Transactional(readOnly = true)
    public AlbumDetailResponse getAlbum(Long albumId) {
        Album album = findPublishedAlbum(albumId);

        // 공개 상세 조회에서 노출 가능한 아티스트만 조회
        Artist artist = findVisibleArtist(album.getArtistId());

        // 앨범에 속한 공개 정식 발매 트랙 목록 조회
        List<AlbumTrackResponse> tracks = trackRepository.searchAlbumTracks(albumId).stream()
                .map(AlbumTrackResponse::from)
                .toList();

        return AlbumDetailResponse.of(album, artist.getName(), tracks);
    }

    /**
     * 아티스트별 앨범 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ArtistAlbumListResponse> getArtistAlbums(Long artistId) {
        // 삭제되지 않은 아티스트 확인
        findNotDeletedArtist(artistId);

        return albumRepository.searchArtistAlbums(artistId).stream()
                .map(ArtistAlbumListResponse::from)
                .toList();
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

    // 공개 조회 가능한 아티스트 조회
    private Artist findVisibleArtist(Long artistId) {
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(AlbumErrorCode.ERR_ALBUM_NOT_FOUND));

        if (artist.isDeleted() || artist.getStatus() != ArtistStatus.ACTIVE) {
            throw new BusinessException(AlbumErrorCode.ERR_ALBUM_NOT_FOUND);
        }

        return artist;
    }

    // 앨범 등록 신청 조회
    private AlbumApplication findAlbumApplication(Long applicationId) {
        return albumApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(
                        AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_NOT_FOUND
                ));
    }

    // 앨범 등록 신청 조회 (비관적 락)
    private AlbumApplication findAlbumApplicationForUpdate(Long applicationId) {
        return albumApplicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new BusinessException(
                        AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_NOT_FOUND
                ));
    }

    // 공개 가능한 앨범 조회
    private Album findPublishedAlbum(Long albumId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new BusinessException(AlbumErrorCode.ERR_ALBUM_NOT_FOUND));

        if (album.getDeletedAt() != null || album.getStatus() != AlbumStatus.PUBLISHED) {
            throw new BusinessException(AlbumErrorCode.ERR_ALBUM_NOT_FOUND);
        }

        return album;
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
                    AlbumApplicationErrorCode.ERR_INACTIVE_ARTIST_CANNOT_REQUEST_ALBUM_APPLICATION
            );
        }
    }

    // 공개 예약 옵션 검증
    private void validatePublishDelayDays(Integer publishDelayDays) {
        if (publishDelayDays == null || publishDelayDays < 0 || publishDelayDays > 7) {
            throw new BusinessException(
                    AlbumApplicationErrorCode.ERR_INVALID_PUBLISH_DELAY_DAYS
            );
        }
    }

    // 중복 신청 검증
    private void validateDuplicateAlbumApplication(Long userId, Long artistId, String title) {
        if (albumApplicationRepository.existsPendingApplication(userId, artistId, title)) {
            throw new BusinessException(
                    AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_ALREADY_EXISTS
            );
        }

        if (albumApplicationRepository.existsApprovedApplication(userId, artistId, title)) {
            throw new BusinessException(
                    AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_ALREADY_PROCESSED
            );
        }
    }

    // 상세 조회 권한 검증
    private void validateAlbumApplicationDetailAccess(
            Long userId,
            User user,
            AlbumApplication application
    ) {
        boolean isRequester = application.getRequesterUserId().equals(userId);
        boolean isAdmin = user.getRole() == UserRole.ADMIN;

        if (!isRequester && !isAdmin) {
            throw new BusinessException(
                    AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_DETAIL_FORBIDDEN
            );
        }
    }

    // =========================
    // 생성 / 후처리
    // =========================

    // 승인된 신청 기반 앨범 생성
    private Album createAlbum(AlbumApplication application) {
        LocalDateTime scheduledPublishAt =
                calculateScheduledPublishAt(application.getPublishDelayDays());

        Album album = Album.create(
                application.getArtistId(),
                application.getTitle(),
                application.getDescription(),
                application.getCoverImageUrl(),
                scheduledPublishAt
        );

        // 즉시 공개
        if (application.getPublishDelayDays() == 0) {
            album.publish();
        }

        return album;
    }

    // 공개 예약 시각 계산
    private LocalDateTime calculateScheduledPublishAt(Integer publishDelayDays) {
        if (publishDelayDays == 0) {
            return null;
        }

        return LocalDateTime.now().plusDays(publishDelayDays);
    }
}