package com.fivefy.domain.artist.dto.request;

import jakarta.validation.constraints.Size;

/**
 * 아티스트 프로필 수정 요청 DTO
 */
public record ArtistProfileUpdateRequest(

        @Size(max = 100, message = "아티스트명은 100자 이하입니다")
        String name,

        @Size(max = 255, message = "소개글은 255자 이하입니다")
        String bio,

        @Size(max = 500, message = "프로필 이미지 URL은 500자 이하입니다")
        String profileImageUrl
) {
}