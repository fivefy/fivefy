package com.fivefy.domain.artist.dto.request;

import com.fivefy.domain.artist.enums.ArtistType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 아티스트 등록 요청 생성 API의 요청 DTO
 */
public record ArtistApplicationCreateRequest(
        @NotBlank(message = "아티스트 이름은 필수입니다")
        @Size(max = 100, message = "아티스트 이름은 100자 이하입니다")
        String requestedName,

        @NotNull(message = "아티스트 타입은 필수입니다")
        ArtistType artistType,

        @Size(max = 1000, message = "소개는 1000자 이하입니다")
        String bio,

        @Size(max = 255, message = "이미지 URL은 255자 이하입니다")
        String profileImageUrl

) {
}