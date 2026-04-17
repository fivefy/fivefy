package com.fivefy.domain.album.service;

import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.album.dto.request.AlbumApplicationCreateRequest;
import com.fivefy.domain.album.dto.response.AlbumApplicationApproveResponse;
import com.fivefy.domain.album.dto.response.AlbumApplicationDetailResponse;
import com.fivefy.domain.album.dto.response.AlbumApplicationListResponse;
import com.fivefy.domain.album.dto.response.AlbumApplicationResponse;
import com.fivefy.domain.album.entity.Album;
import com.fivefy.domain.album.entity.AlbumApplication;
import com.fivefy.domain.album.enums.AlbumApplicationErrorCode;
import com.fivefy.domain.album.repository.AlbumApplicationRepository;
import com.fivefy.domain.album.repository.AlbumRepository;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.artist.enums.ArtistErrorCode;
import com.fivefy.domain.artist.enums.ArtistStatus;
import com.fivefy.domain.artist.repository.ArtistRepository;
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

    private final AlbumApplicationRepository AlbumApplicationRepository;
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final UserRepository userRepository;

    /**
     * 앨범 등록 요청 생성
     */
    @Transactional
    public AlbumApplicationResponse createAlbumApplication(
            Long userId,
            AlbumApplicationCreateRequest request
    ) {
        // 요청 유저 존재 확인
        findUser(userId);

        // 아티스트 조회 및 삭제 여부 검증
        Artist artist = findNotDeletedArtist(request.artistId());

        // 소유자 검증
        validateArtistOwner(userId, artist);

        // 아티스트 상태 검증
        validateArtistActive(artist);

        // 공개 예약 정책 검증
        validatePublishDelayDays(request.publishDelayDays());

        // 중복 요청 검증
        validateDuplicatePendingRequest(userId, request.artistId(), request.title());

        // 등록 요청 생성 및 저장
        AlbumApplication savedRequest = AlbumApplicationRepository.save(
                AlbumApplication.create(
                        userId,
                        request.artistId(),
                        request.title(),
                        request.description(),
                        request.coverImageUrl(),
                        request.publishDelayDays()
                )
        );

        return AlbumApplicationResponse.from(savedRequest);
    }

    /**
     * 내 앨범 등록 요청 목록 조회
     */
    @Transactional(readOnly = true)
    public List<AlbumApplicationResponse> getMyAlbumApplications(Long userId) {
        findUser(userId);

        return AlbumApplicationRepository.searchMyAlbumApplications(userId)
                .stream()
                .map(AlbumApplicationResponse::from)
                .toList();
    }

    /**
     * 앨범 등록 요청 상세 조회
     */
    @Transactional(readOnly = true)
    public AlbumApplicationDetailResponse getAlbumApplication(Long userId, Long requestId) {
        AlbumApplication request = findAlbumApplication(requestId);
        User user = findUser(userId);

        // 요청자 또는 관리자만 조회 가능
        validateAlbumApplicationDetailAccess(userId, user, request);

        return AlbumApplicationDetailResponse.from(request);
    }

    /**
     * 앨범 등록 요청 목록 조회 (관리자)
     */
    @Transactional(readOnly = true)
    public PageResponse<AlbumApplicationListResponse> getAlbumApplications(
            ApplicationStatus status,
            Pageable pageable
    ) {
        Page<AlbumApplication> page =
                AlbumApplicationRepository.searchAlbumApplications(status, pageable);

        return PageResponse.from(
                page.map(AlbumApplicationListResponse::from)
        );
    }

    /**
     * 앨범 등록 요청 승인
     */
    @Transactional
    public AlbumApplicationApproveResponse approveAlbumApplication(Long adminId, Long requestId) {
        AlbumApplication request = findAlbumApplication(requestId);

        // 상태 전이는 엔티티에 위임
        request.approve(adminId);

        // 승인 요청 기반 실제 앨범 생성
        Album savedAlbum = albumRepository.save(createAlbum(request));

        return AlbumApplicationApproveResponse.from(request, savedAlbum.getId());
    }

    // =========================
    // 조회
    // =========================

    // 유저 조회
    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.ERR_USER_NOT_FOUND));
    }

    // 아티스트 조회
    private Artist findArtist(Long artistId) {
        return artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ArtistErrorCode.ERR_ARTIST_NOT_FOUND));
    }

    /**
     * 삭제되지 않은 아티스트 조회
     */
    private Artist findNotDeletedArtist(Long artistId) {
        Artist artist = findArtist(artistId);

        if (artist.isDeleted()) {
            throw new BusinessException(ArtistErrorCode.ERR_ARTIST_NOT_FOUND);
        }

        return artist;
    }

    // 앨범 등록 요청 조회
    private AlbumApplication findAlbumApplication(Long requestId) {
        return AlbumApplicationRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(
                        AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_NOT_FOUND
                ));
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

    /**
     * 공개 예약 옵션 검증
     */
    private void validatePublishDelayDays(Integer publishDelayDays) {
        if (publishDelayDays == null || publishDelayDays < 0 || publishDelayDays > 7) {
            throw new BusinessException(
                    AlbumApplicationErrorCode.ERR_INVALID_PUBLISH_DELAY_DAYS
            );
        }
    }

    // 중복 요청 검증
    private void validateDuplicatePendingRequest(Long userId, Long artistId, String title) {
        if (AlbumApplicationRepository.existsPendingRequest(userId, artistId, title)) {
            throw new BusinessException(
                    AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_ALREADY_EXISTS
            );
        }
    }

    // 상세 조회 권한 검증
    private void validateAlbumApplicationDetailAccess(
            Long userId,
            User user,
            AlbumApplication request
    ) {
        boolean isRequester = request.getRequesterUserId().equals(userId);
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

    // 승인 요청 기반 앨범 생성
    private Album createAlbum(AlbumApplication request) {
        LocalDateTime scheduledPublishAt =
                calculateScheduledPublishAt(request.getPublishDelayDays());

        Album album = Album.create(
                request.getArtistId(),
                request.getTitle(),
                request.getDescription(),
                request.getCoverImageUrl(),
                scheduledPublishAt
        );

        // 즉시 공개
        if (request.getPublishDelayDays() == 0) {
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