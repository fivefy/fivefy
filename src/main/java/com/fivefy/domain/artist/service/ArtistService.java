package com.fivefy.domain.artist.service;

import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.domain.artist.dto.request.ArtistApplicationCreateRequest;
import com.fivefy.domain.artist.dto.response.ArtistApplicationListResponse;
import com.fivefy.domain.artist.dto.response.ArtistApplicationResponse;
import com.fivefy.domain.artist.entity.ArtistApplication;
import com.fivefy.domain.artist.repository.ArtistApplicationRepository;
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
     *
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
}