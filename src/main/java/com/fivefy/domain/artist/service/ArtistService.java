package com.fivefy.domain.artist.service;

import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.artist.dto.request.ArtistApplicationCreateRequest;
import com.fivefy.domain.artist.dto.request.ArtistApplicationRejectRequest;
import com.fivefy.domain.artist.dto.request.ArtistProfileUpdateRequest;
import com.fivefy.domain.artist.dto.response.*;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.artist.entity.ArtistApplication;
import com.fivefy.domain.artist.enums.ArtistApplicationErrorCode;
import com.fivefy.domain.artist.enums.ArtistErrorCode;
import com.fivefy.domain.artist.enums.ArtistType;
import com.fivefy.domain.artist.repository.ArtistApplicationRepository;
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

import java.util.List;

/**
 * 아티스트 도메인 서비스
 */
@Service
@RequiredArgsConstructor
public class ArtistService {

    private final ArtistApplicationRepository artistApplicationRepository;
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;

    /**
     * 아티스트 등록 신청 생성
     */
    @Transactional
    public ArtistApplicationResponse createArtistApplication(
            Long userId,
            ArtistApplicationCreateRequest request
    ) {
        // 신청 유저 존재 확인
        findUser(userId);

        // 중복 신청 검증
        validateDuplicateActiveApplication(userId, request.requestedName(), request.artistType());

        // 등록 신청 생성 및 저장
        ArtistApplication savedApplication = artistApplicationRepository.save(
                ArtistApplication.create(
                        userId,
                        request.requestedName(),
                        request.artistType(),
                        request.bio(),
                        request.profileImageUrl()
                )
        );

        return ArtistApplicationResponse.from(savedApplication);
    }

    /**
     * 내 아티스트 등록 신청 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ArtistApplicationResponse> getMyArtistApplications(Long userId) {
        findUser(userId);

        return artistApplicationRepository.searchMyArtistApplications(userId)
                .stream()
                .map(ArtistApplicationResponse::from)
                .toList();
    }

    /**
     * 아티스트 등록 신청 목록 조회 (관리자)
     */
    @Transactional(readOnly = true)
    public PageResponse<ArtistApplicationListResponse> getArtistApplications(
            ApplicationStatus status,
            Pageable pageable
    ) {
        Page<ArtistApplication> page =
                artistApplicationRepository.searchArtistApplications(status, pageable);

        return PageResponse.from(
                page.map(ArtistApplicationListResponse::from)
        );
    }

    /**
     * 아티스트 등록 신청 상세 조회
     */
    @Transactional(readOnly = true)
    public ArtistApplicationDetailResponse getArtistApplication(Long userId, Long applicationId) {
        ArtistApplication application = findArtistApplication(applicationId);
        User user = findUser(userId);

        // 신청자 또는 관리자만 조회 가능
        validateArtistApplicationDetailAccess(userId, user, application);

        return ArtistApplicationDetailResponse.from(application);
    }

    /**
     * 아티스트 등록 신청 승인 (관리자)
     */
    @Transactional
    public ArtistApplicationApproveResponse approveArtistApplication(Long adminId, Long applicationId) {
        ArtistApplication application = findArtistApplication(applicationId);

        // 상태 전이는 엔티티에 위임
        application.approve(adminId);

        // 승인된 신청 기반 아티스트 생성
        Artist savedArtist = artistRepository.save(createArtist(application));

        return ArtistApplicationApproveResponse.from(application, savedArtist.getId());
    }

    /**
     * 아티스트 등록 신청 거절 (관리자)
     */
    @Transactional
    public ArtistApplicationRejectResponse rejectArtistApplication(
            Long adminId,
            Long applicationId,
            ArtistApplicationRejectRequest request
    ) {
        ArtistApplication application = findArtistApplication(applicationId);

        // 상태 전이는 엔티티에 위임
        application.reject(adminId, request.rejectionReason());

        return ArtistApplicationRejectResponse.from(application);
    }

    /**
     * 내 아티스트 목록 조회
     */
    @Transactional(readOnly = true)
    public List<MyArtistResponse> getMyArtists(Long userId) {
        findUser(userId);

        return artistRepository.findMyArtists(userId).stream()
                .map(MyArtistResponse::from)
                .toList();
    }

    /**
     * 아티스트 상세 조회
     */
    @Transactional(readOnly = true)
    public ArtistDetailResponse getArtist(Long artistId) {
        Artist artist = findNotDeletedArtist(artistId);

        return ArtistDetailResponse.from(artist);
    }

    /**
     * 아티스트 프로필 수정
     */
    @Transactional
    public ArtistDetailResponse updateArtistProfile(
            Long userId,
            Long artistId,
            ArtistProfileUpdateRequest request
    ) {
        Artist artist = findNotDeletedArtist(artistId);

        // 소유자 검증
        validateArtistOwner(userId, artist);

        // 프로필 수정
        artist.updateProfile(
                request.name(),
                request.bio(),
                request.profileImageUrl()
        );

        return ArtistDetailResponse.from(artist);
    }

    /**
     * 아티스트 삭제
     */
    @Transactional
    public void deleteArtist(Long userId, Long artistId) {
        Artist artist = findNotDeletedArtist(artistId);

        // 소유자 검증
        validateArtistOwner(userId, artist);

        artist.softDelete();
    }

    /**
     * 아티스트 활성화
     */
    @Transactional
    public ArtistDetailResponse activateArtist(Long userId, Long artistId) {
        Artist artist = findNotDeletedArtist(artistId);

        // 소유자 검증
        validateArtistOwner(userId, artist);

        artist.activate();

        return ArtistDetailResponse.from(artist);
    }

    /**
     * 아티스트 비활성화
     */
    @Transactional
    public ArtistDetailResponse deactivateArtist(Long userId, Long artistId) {
        Artist artist = findNotDeletedArtist(artistId);

        // 소유자 검증
        validateArtistOwner(userId, artist);

        artist.deactivate();

        return ArtistDetailResponse.from(artist);
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

    // 삭제되지 않은 아티스트 조회
    private Artist findNotDeletedArtist(Long artistId) {
        Artist artist = findArtist(artistId);

        if (artist.isDeleted()) {
            throw new BusinessException(ArtistErrorCode.ERR_ARTIST_NOT_FOUND);
        }

        return artist;
    }

    // 아티스트 등록 신청 조회
    private ArtistApplication findArtistApplication(Long applicationId) {
        return artistApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(
                        ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_NOT_FOUND
                ));
    }

    // =========================
    // 검증
    // =========================

    // 중복 신청 검증
    private void validateDuplicateActiveApplication(Long userId, String requestedName, ArtistType artistType) {
        if (artistApplicationRepository.existsActiveApplication(userId, requestedName, artistType)) {
            throw new BusinessException(
                    ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_ALREADY_EXISTS
            );
        }
    }

    // 상세 조회 권한 검증
    private void validateArtistApplicationDetailAccess(
            Long userId,
            User user,
            ArtistApplication application
    ) {
        boolean isRequester = application.getRequesterUserId().equals(userId);
        boolean isAdmin = user.getRole() == UserRole.ADMIN;

        if (!isRequester && !isAdmin) {
            throw new BusinessException(
                    ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_DETAIL_FORBIDDEN
            );
        }
    }

    // 아티스트 소유자 검증
    private void validateArtistOwner(Long userId, Artist artist) {
        if (!artist.isOwnedBy(userId)) {
            throw new BusinessException(ArtistErrorCode.ERR_FORBIDDEN_ARTIST_ACCESS);
        }
    }

    // =========================
    // 생성 / 후처리
    // =========================

    // 승인된 신청 기반 아티스트 생성
    private Artist createArtist(ArtistApplication application) {
        return Artist.create(
                application.getRequesterUserId(),
                application.getRequestedName(),
                application.getArtistType(),
                application.getBio(),
                application.getProfileImageUrl()
        );
    }
}