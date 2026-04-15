package com.fivefy.domain.artist.service;

import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.artist.dto.request.ArtistApplicationCreateRequest;
import com.fivefy.domain.artist.dto.response.ArtistApplicationDetailResponse;
import com.fivefy.domain.artist.dto.response.ArtistApplicationListResponse;
import com.fivefy.domain.artist.dto.response.ArtistApplicationResponse;
import com.fivefy.domain.artist.entity.ArtistApplication;
import com.fivefy.domain.artist.enums.ArtistApplicationErrorCode;
import com.fivefy.domain.artist.repository.ArtistApplicationRepository;
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

    /**
     * 아티스트 등록 요청 생성
     *
     * 1. 요청 엔티티 생성
     * 2. DB 저장
     * 3. 응답 DTO 반환
     */
    @Transactional
    public ArtistApplicationResponse createArtistApplication(
            Long userId, ArtistApplicationCreateRequest request) {

        // 1. 아티스트 등록 요청 엔티티 생성
        ArtistApplication application = ArtistApplication.create(
                userId,
                request.requestedName(),
                request.bio(),
                request.profileImageUrl()
        );

        // 2. DB 저장
        ArtistApplication savedApplication = artistApplicationRepository.save(application);

        // 3. 응답 DTO 반환
        return ArtistApplicationResponse.from(savedApplication);
    }

    /**
     * 내 아티스트 등록 요청 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ArtistApplicationResponse> getMyArtistApplications(Long userId) {
        return artistApplicationRepository.findAllByRequesterUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ArtistApplicationResponse::from)
                .toList();
    }

    /**
     * 아티스트 등록 요청 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<ArtistApplicationListResponse> getArtistApplications(Pageable pageable) {
        // Querydsl 기반으로 아티스트 등록 요청을 최신순으로 조회한다.
        Page<ArtistApplication> page = artistApplicationRepository.searchArtistApplications(pageable);

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
        ArtistApplication application =  artistApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(
                        ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_NOT_FOUND)
        );
        // 현재 로그인 사용자의 역할을 확인하기 위해 유저를 조회한다.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.ERR_USER_NOT_FOUND));

        // 요청자 본인 또는 관리자만 상세 조회할 수 있다.
        boolean isRequester = application.getRequesterUserId().equals(userId);
        boolean isAdmin = user.getRole() == UserRole.ADMIN;

        if (!isRequester && !isAdmin) {
            throw new BusinessException(
                    ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_DETAIL_FORBIDDEN);
        }

        // 조회한 엔티티를 상세 응답 DTO로 변환해 반환한다.
        return ArtistApplicationDetailResponse.from(application);
    }
}