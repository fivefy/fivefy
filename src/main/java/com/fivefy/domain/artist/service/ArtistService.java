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

@Service
@RequiredArgsConstructor
public class ArtistService {

    private final ArtistApplicationRepository artistApplicationRepository;
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;

    /**
     * 아티스트 등록 요청 생성
     */
    @Transactional
    public ArtistApplicationResponse createArtistApplication(
            Long userId, ArtistApplicationCreateRequest request) {

        // 생성 요청 유저가 존재하는지 확인한다.
        findUser(userId);

        // 동일한 이름의 진행 중이거나 승인된 등록 요청 검증
        validateDuplicateActiveApplication(userId, request.requestedName(), request.artistType());

        // 아티스트 등록 요청 엔티티 생성
        ArtistApplication application = ArtistApplication.create(
                userId,
                request.requestedName(),
                request.artistType(),
                request.bio(),
                request.profileImageUrl()
        );

        // DB 저장
        ArtistApplication savedApplication = artistApplicationRepository.save(application);

        // 응답 DTO 반환
        return ArtistApplicationResponse.from(savedApplication);
    }

    /**
     * 내 아티스트 등록 요청 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ArtistApplicationResponse> getMyArtistApplications(Long userId) {

        // 조회 요청 유저가 존재하는지 확인한다.
        findUser(userId);

        return artistApplicationRepository.findAllByRequesterUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ArtistApplicationResponse::from)
                .toList();
    }

    /**
     * 아티스트 등록 요청 목록 조회 (관리자)
     */
    @Transactional(readOnly = true)
    public PageResponse<ArtistApplicationListResponse> getArtistApplications(
            ApplicationStatus status,
            Pageable pageable
    ) {
        // Querydsl 기반으로 상태 조건과 정렬 조건을 적용하여 조회한다.
        Page<ArtistApplication> page =
                artistApplicationRepository.searchArtistApplications(status, pageable);

        // 엔티티 목록을 관리자용 응답 DTO 페이지로 변환한다.
        Page<ArtistApplicationListResponse> response =
                page.map(ArtistApplicationListResponse::from);

        // 공통 페이징 응답 객체로 변환한다.
        return PageResponse.from(response);
    }

    /**
     * 아티스트 등록 요청 상세 조회
     */
    @Transactional(readOnly = true)
    public ArtistApplicationDetailResponse getArtistApplication(Long userId, Long applicationId) {
        // 아티스트 등록 요청을 조회한다.
        ArtistApplication application = findArtistApplication(applicationId);

        // 현재 로그인 사용자의 역할을 확인하기 위해 유저를 조회한다.
        User user = findUser(userId);

        // 요청자 본인 또는 관리자만 상세 조회할 수 있다.
        validateArtistApplicationDetailAccess(userId, user, application);

        // 조회한 엔티티를 상세 응답 DTO로 변환해 반환한다.
        return ArtistApplicationDetailResponse.from(application);
    }

    /**
     * 아티스트 등록 요청 승인
     */
    @Transactional
    public ArtistApplicationApproveResponse approveArtistApplication(Long adminId, Long applicationId) {
        // 승인할 아티스트 등록 요청을 조회한다.
        ArtistApplication application = findArtistApplication(applicationId);

        // 등록 요청이 대기 상태인지 검증한다.
        validateProcessableApplication(application);

        // 아티스트 등록 요청을 승인 상태로 변경한다.
        application.approve(adminId);

        // 승인된 요청 정보로 실제 아티스트를 생성한다.
        Artist savedArtist = artistRepository.save(createArtist(application));

        // 승인 결과를 응답 DTO로 변환해 반환한다.
        return ArtistApplicationApproveResponse.from(application, savedArtist.getId());
    }

    /**
     * 아티스트 등록 요청 거절
     */
    @Transactional
    public ArtistApplicationRejectResponse rejectArtistApplication(
            Long adminId,
            Long applicationId,
            ArtistApplicationRejectRequest request
    ) {
        // 거절할 아티스트 등록 요청을 조회한다.
        ArtistApplication application = findArtistApplication(applicationId);

        // 등록 요청이 대기 상태인지 검증한다.
        validateProcessableApplication(application);

        // 아티스트 등록 요청을 거절 상태로 변경한다.
        application.reject(adminId, request.rejectionReason());

        // 거절 결과를 응답 DTO로 변환해 반환한다.
        return ArtistApplicationRejectResponse.from(application);
    }

    /**
     * 내 아티스트 목록 조회
     */
    @Transactional(readOnly = true)
    public List<MyArtistResponse> getMyArtists(Long userId) {

        // 조회 요청 유저가 존재하는지 확인한다.
        findUser(userId);

        // Querydsl 기반 조회
        List<Artist> artists = artistRepository.findMyArtists(userId);

        // DTO 변환
        return artists.stream()
                .map(MyArtistResponse::from)
                .toList();
    }

    /**
     * 아티스트 상세 조회
     */
    @Transactional(readOnly = true)
    public ArtistDetailResponse getArtist(Long artistId) {
        // 조회할 아티스트를 조회하고 삭제 여부를 검증한다.
        Artist artist = findNotDeletedArtist(artistId);

        // 조회한 엔티티를 상세 응답 DTO로 변환해 반환한다.
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
        // 수정할 아티스트를 조회하고 삭제 여부를 검증한다.
        Artist artist = findNotDeletedArtist(artistId);

        // 아티스트 소유자만 프로필을 수정할 수 있다.
        validateArtistOwner(userId, artist);

        // 요청값 기준으로 아티스트 프로필을 수정한다.
        artist.updateProfile(
                request.name(),
                request.bio(),
                request.profileImageUrl()
        );

        // 수정된 엔티티를 상세 응답 DTO로 변환해 반환한다.
        return ArtistDetailResponse.from(artist);
    }

    /**
     * 아티스트 삭제
     */
    @Transactional
    public void deleteArtist(Long userId, Long artistId) {
        // 삭제할 아티스트를 조회한다.
        Artist artist = findArtist(artistId);

        // 이미 삭제된 아티스트는 예외를 발생시킨다.
        validateNotDeleted(artist);

        // 아티스트 소유자만 삭제할 수 있다.
        validateArtistOwner(userId, artist);

        // 아티스트를 soft delete 처리한다.
        artist.softDelete();
    }

    /**
     * 아티스트 활성화
     */
    @Transactional
    public ArtistDetailResponse activateArtist(Long userId, Long artistId) {
        // 활성화할 아티스트를 조회하고 삭제 여부를 검증한다.
        Artist artist = findNotDeletedArtist(artistId);

        // 아티스트 소유자만 활성화할 수 있다.
        validateArtistOwner(userId, artist);

        // 아티스트를 활성 상태로 변경한다.
        artist.activate();

        // 변경된 엔티티를 상세 응답 DTO로 변환해 반환한다.
        return ArtistDetailResponse.from(artist);
    }

    /**
     * 아티스트 비활성화
     */
    @Transactional
    public ArtistDetailResponse deactivateArtist(Long userId, Long artistId) {
        // 비활성화할 아티스트를 조회하고 삭제 여부를 검증한다.
        Artist artist = findNotDeletedArtist(artistId);

        // 아티스트 소유자만 비활성화할 수 있다.
        validateArtistOwner(userId, artist);

        // 아티스트를 비활성 상태로 변경한다.
        artist.deactivate();

        // 변경된 엔티티를 상세 응답 DTO로 변환해 반환한다.
        return ArtistDetailResponse.from(artist);
    }

    /**
     * 동일 이름의 진행 중인 아티스트 등록 요청 중복 검증
     */
    private void validateDuplicateActiveApplication(Long userId, String requestedName, ArtistType artistType) {
        // 동일 유저의 동일 이름 진행 중이거나 승인된 요청이 있는지 확인한다.
        boolean existsActiveApplication =
                artistApplicationRepository.existsActiveApplication(userId, requestedName, artistType);

        // 다시 신청할 수 없는 상태의 요청이 있으면 예외를 발생시킨다.
        if (existsActiveApplication) {
            throw new BusinessException(
                    ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_ALREADY_EXISTS
            );
        }
    }

    /**
     * 아티스트 등록 요청 단건 조회
     */
    private ArtistApplication findArtistApplication(Long applicationId) {
        // 아티스트 등록 요청을 조회한다.
        return artistApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(
                        ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_NOT_FOUND)
                );
    }

    /**
     * 유저 단건 조회
     */
    private User findUser(Long userId) {
        // 현재 로그인 유저를 조회한다.
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.ERR_USER_NOT_FOUND));
    }

    /**
     * 아티스트 등록 요청 상세 조회 권한 검증
     */
    private void validateArtistApplicationDetailAccess(
            Long userId,
            User user,
            ArtistApplication application
    ) {
        // 요청자 본인 여부를 확인한다.
        boolean isRequester = application.getRequesterUserId().equals(userId);

        // 관리자 여부를 확인한다.
        boolean isAdmin = user.getRole() == UserRole.ADMIN;

        // 요청자 본인도 관리자도 아니면 예외를 발생시킨다.
        if (!isRequester && !isAdmin) {
            throw new BusinessException(
                    ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_DETAIL_FORBIDDEN);
        }
    }

    /**
     * 승인된 요청 정보로 실제 아티스트 생성
     */
    private Artist createArtist(ArtistApplication application) {
        // 승인된 요청 정보로 실제 아티스트 엔티티를 생성한다.
        return Artist.create(
                application.getRequesterUserId(),
                application.getRequestedName(),
                application.getArtistType(),
                application.getBio(),
                application.getProfileImageUrl()
        );
    }

    /**
     * 아티스트 등록 요청 처리 가능 상태 검증 (PENDING만 허용)
     */
    private void validateProcessableApplication(ArtistApplication application) {
        if (!application.isPending()) {
            throw new BusinessException(
                    ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_ALREADY_PROCESSED
            );
        }
    }

    /**
     * 아티스트 단건 조회
     */
    private Artist findArtist(Long artistId) {
        // 아티스트를 조회한다.
        return artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ArtistErrorCode.ERR_ARTIST_NOT_FOUND));
    }

    /**
     * 삭제되지 않은 아티스트 단건 조회
     */
    private Artist findNotDeletedArtist(Long artistId) {
        // 아티스트를 조회한다.
        Artist artist = findArtist(artistId);

        // 삭제된 아티스트면 예외를 발생시킨다.
        if (artist.isDeleted()) {
            throw new BusinessException(ArtistErrorCode.ERR_ARTIST_NOT_FOUND);
        }

        return artist;
    }

    /**
     * 아티스트 소유자 검증
     */
    private void validateArtistOwner(Long userId, Artist artist) {
        // 아티스트 소유자가 아니면 예외를 발생시킨다.
        if (!artist.isOwnedBy(userId)) {
            throw new BusinessException(ArtistErrorCode.ERR_FORBIDDEN_ARTIST_ACCESS);
        }
    }

    /**
     * 이미 삭제된 아티스트 검증
     */
    private void validateNotDeleted(Artist artist) {
        // 이미 삭제된 아티스트면 예외를 발생시킨다.
        if (artist.isDeleted()) {
            throw new BusinessException(ArtistErrorCode.ERR_ARTIST_ALREADY_DELETED);
        }
    }
}