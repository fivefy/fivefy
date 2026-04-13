package com.fivefy.domain.artist.service;

import com.fivefy.domain.artist.dto.request.ArtistApplicationCreateRequest;
import com.fivefy.domain.artist.dto.response.ArtistApplicationCreateResponse;
import com.fivefy.domain.artist.entity.ArtistApplication;
import com.fivefy.domain.artist.repository.ArtistApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public ArtistApplicationCreateResponse createArtistApplication(
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
        return ArtistApplicationCreateResponse.from(savedApplication);
    }
}