package com.fivefy.domain.album.dto.request;

import jakarta.validation.constraints.*;

/**
 * 앨범 등록 신청 생성 요청 DTO
 */
public record AlbumApplicationCreateRequest(

        @NotNull(message = "아티스트 ID는 필수입니다.")
        Long artistId,

        @NotBlank(message = "앨범 제목은 필수입니다.")
        @Size(max = 150, message = "앨범 제목은 150자 이하여야 합니다.")
        String title,

        String description,

        @Size(max = 255, message = "커버 이미지 URL은 255자 이하여야 합니다.")
        String coverImageUrl,

        @NotNull(message = "공개 예약 옵션은 필수입니다.")
        @Min(value = 0, message = "공개 예약 옵션은 0 이상이어야 합니다.")
        @Max(value = 7, message = "공개 예약 옵션은 7 이하여야 합니다.")
        Integer publishDelayDays

) {
}