package com.fivefy.domain.track.dto.request;

import jakarta.validation.constraints.*;

/**
 * 정식 발매 트랙 등록 신청 요청 DTO
 */
public record OfficialTrackApplicationCreateRequest(
        @NotNull(message = "아티스트 ID는 필수입니다")
        Long artistId,

        @NotNull(message = "앨범 ID는 필수입니다")
        Long albumId,

        @NotNull(message = "트랙 번호는 필수입니다")
        @Positive(message = "트랙 번호는 1 이상이어야 합니다")
        Long trackNumber,

        @NotBlank(message = "트랙 제목은 필수입니다")
        @Size(max = 150, message = "트랙 제목은 150자 이하여야 합니다")
        String title,

        String lyrics,

        @NotBlank(message = "장르는 필수입니다")
        @Size(max = 100, message = "장르는 100자 이하여야 합니다")
        String genre,

        @NotBlank(message = "오디오 URL은 필수입니다")
        @Size(max = 255, message = "오디오 URL은 255자 이하여야 합니다")
        String audioUrl,

        @NotNull(message = "재생 시간은 필수입니다")
        @Positive(message = "재생 시간은 1초 이상이어야 합니다")
        Long durationSec,

        @Size(max = 255, message = "피처링 아티스트 정보는 255자 이하여야 합니다")
        String featuredArtistText,

        @NotNull(message = "공개 예약 옵션은 필수입니다")
        @Min(value = 0, message = "공개 예약 옵션은 0 이상이어야 합니다")
        @Max(value = 7, message = "공개 예약 옵션은 7 이하여야 합니다")
        Integer publishDelayDays
) {
}