package com.fivefy.domain.artist.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.domain.artist.dto.request.ArtistApplicationCreateRequest;
import com.fivefy.domain.artist.dto.response.ArtistApplicationCreateResponse;
import com.fivefy.domain.artist.service.ArtistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ArtistController {

    private final ArtistService artistService;

    /**
     * 아티스트 등록 요청 생성 API
     *
     * 현재는 로그인 사용자 식별을 임시로 Header에서 받고 있지만,
     * 이후 Security 적용 시 인증 정보에서 꺼내는 방식으로 바꿀 수 있음.
     */
    /**
     * 아티스트 등록 요청 생성 API
     */
    @PostMapping("/artists/applications")
    public ResponseEntity<BaseResponse<ArtistApplicationCreateResponse>> createArtistApplication(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ArtistApplicationCreateRequest request
    ) {
        ArtistApplicationCreateResponse response = artistService.createArtistApplication(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                BaseResponse.success(HttpStatus.CREATED, "아티스트 등록 요청 성공", response)
        );
    }
}