package com.fivefy.domain.artist.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.domain.artist.dto.request.ArtistApplicationCreateRequest;
import com.fivefy.domain.artist.dto.response.ArtistApplicationCreateResponse;
import com.fivefy.domain.artist.dto.response.ArtistApplicationGetResponse;
import com.fivefy.domain.artist.service.ArtistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ArtistController {

    private final ArtistService artistService;

    /**
     * 아티스트 등록 요청 생성 API
     */
    @PostMapping("/artist-applications")
    public ResponseEntity<BaseResponse<ArtistApplicationCreateResponse>> createArtistApplication(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ArtistApplicationCreateRequest request
    ) {
        ArtistApplicationCreateResponse response = artistService.createArtistApplication(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                BaseResponse.success(HttpStatus.CREATED, "아티스트 등록 요청 성공", response)
        );
    }

    /**
     * 내 아티스트 등록 요청 목록 조회 API
     */
    @GetMapping("/artist-applications/me")
    public ResponseEntity<BaseResponse<List<ArtistApplicationGetResponse>>> getMyArtistApplications(
            @AuthenticationPrincipal Long userId
    ) {
        List<ArtistApplicationGetResponse> response = artistService.getMyArtistApplications(userId);

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "내 아티스트 등록 요청 목록 조회 성공", response)
        );
    }
}