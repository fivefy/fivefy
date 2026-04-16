package com.fivefy.domain.artist.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.artist.dto.request.ArtistProfileUpdateRequest;
import com.fivefy.domain.artist.dto.response.ArtistDetailResponse;
import com.fivefy.domain.artist.dto.response.MyArtistResponse;
import com.fivefy.domain.artist.enums.ArtistStatus;
import com.fivefy.domain.artist.enums.ArtistType;
import com.fivefy.domain.artist.service.ArtistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtistControllerTest {

    @Mock
    private ArtistService artistService;

    @InjectMocks
    private ArtistController artistController;

    @Nested
    @DisplayName("아티스트 등록 요청 API")
    class CreateArtistApplication {

        @Test
        @DisplayName("아티스트 등록 요청에 성공한다")
        void createArtistApplication_success() {
            // given
            Long userId = 1L;

            when(artistService.createArtistApplication(userId, null))
                    .thenReturn(null);

            // when
            ResponseEntity<?> response = artistController.createArtistApplication(userId, null);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();

            verify(artistService, times(1)).createArtistApplication(userId, null);
        }
    }

    @Nested
    @DisplayName("내 아티스트 등록 요청 목록 조회 API")
    class GetMyArtistApplications {

        @Test
        @DisplayName("내 아티스트 등록 요청 목록 조회에 성공한다")
        void getMyArtistApplications_success() {
            // given
            Long userId = 1L;

            when(artistService.getMyArtistApplications(userId))
                    .thenReturn(List.of());

            // when
            ResponseEntity<?> response = artistController.getMyArtistApplications(userId);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            verify(artistService, times(1)).getMyArtistApplications(userId);
        }
    }

    @Nested
    @DisplayName("관리자 아티스트 등록 요청 목록 조회 API")
    class GetArtistApplications {

        @Test
        @DisplayName("관리자는 아티스트 등록 요청 목록 조회에 성공한다")
        void getArtistApplications_success() {
            // given
            ApplicationStatus status = ApplicationStatus.PENDING;
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt"));

            when(artistService.getArtistApplications(status, pageable))
                    .thenReturn(null);

            // when
            ResponseEntity<?> response = artistController.getArtistApplications(status, pageable);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            verify(artistService, times(1)).getArtistApplications(status, pageable);
        }
    }

    @Nested
    @DisplayName("아티스트 등록 요청 상세 조회 API")
    class GetArtistApplication {

        @Test
        @DisplayName("아티스트 등록 요청 상세 조회에 성공한다")
        void getArtistApplication_success() {
            // given
            Long userId = 1L;
            Long applicationId = 10L;

            when(artistService.getArtistApplication(userId, applicationId))
                    .thenReturn(null);

            // when
            ResponseEntity<?> response = artistController.getArtistApplication(userId, applicationId);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            verify(artistService, times(1)).getArtistApplication(userId, applicationId);
        }
    }

    @Nested
    @DisplayName("아티스트 등록 요청 승인 API")
    class ApproveArtistApplication {

        @Test
        @DisplayName("아티스트 등록 요청 승인에 성공한다")
        void approveArtistApplication_success() {
            // given
            Long adminId = 1L;
            Long applicationId = 10L;

            when(artistService.approveArtistApplication(adminId, applicationId))
                    .thenReturn(null);

            // when
            ResponseEntity<?> response = artistController.approveArtistApplication(adminId, applicationId);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            verify(artistService, times(1)).approveArtistApplication(adminId, applicationId);
        }
    }

    @Nested
    @DisplayName("아티스트 등록 요청 거절 API")
    class RejectArtistApplication {

        @Test
        @DisplayName("아티스트 등록 요청 거절에 성공한다")
        void rejectArtistApplication_success() {
            // given
            Long adminId = 1L;
            Long applicationId = 10L;

            when(artistService.rejectArtistApplication(adminId, applicationId, null))
                    .thenReturn(null);

            // when
            ResponseEntity<?> response = artistController.rejectArtistApplication(adminId, applicationId, null);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            verify(artistService, times(1)).rejectArtistApplication(adminId, applicationId, null);
        }
    }

    @Nested
    @DisplayName("내 아티스트 목록 조회 API")
    class GetMyArtists {

        @Test
        @DisplayName("내 아티스트 목록 조회에 성공한다")
        void getMyArtists_success() {
            // given
            Long userId = 1L;

            MyArtistResponse firstResponse = new MyArtistResponse(
                    2L,
                    "아이유",
                    ArtistType.SOLO.name(),
                    "가수",
                    "https://example.com/iu.jpg",
                    LocalDateTime.of(2026, 4, 15, 10, 0, 0),
                    LocalDateTime.of(2026, 4, 15, 11, 0, 0)
            );

            MyArtistResponse secondResponse = new MyArtistResponse(
                    1L,
                    "아이유 밴드",
                    ArtistType.COLLABORATION.name(),
                    "프로젝트 아티스트",
                    "https://example.com/band.jpg",
                    LocalDateTime.of(2026, 4, 14, 10, 0, 0),
                    LocalDateTime.of(2026, 4, 14, 11, 0, 0)
            );

            when(artistService.getMyArtists(userId))
                    .thenReturn(List.of(firstResponse, secondResponse));

            // when
            ResponseEntity<BaseResponse<List<MyArtistResponse>>> response =
                    artistController.getMyArtists(userId);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            verify(artistService, times(1)).getMyArtists(userId);
        }
    }

    @Nested
    @DisplayName("아티스트 상세 조회 API")
    class GetArtist {

        @Test
        @DisplayName("아티스트 상세 조회에 성공한다")
        void getArtist_success() {
            // given
            Long artistId = 1L;

            ArtistDetailResponse detailResponse = new ArtistDetailResponse(
                    artistId,
                    "아이유",
                    ArtistType.SOLO.name(),
                    ArtistStatus.ACTIVE.name(),
                    "가수",
                    "https://example.com/iu.jpg",
                    LocalDateTime.of(2026, 4, 15, 10, 0, 0),
                    LocalDateTime.of(2026, 4, 15, 11, 0, 0)
            );

            when(artistService.getArtist(artistId))
                    .thenReturn(detailResponse);

            // when
            ResponseEntity<BaseResponse<ArtistDetailResponse>> response =
                    artistController.getArtist(artistId);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            verify(artistService, times(1)).getArtist(artistId);
        }
    }

    @Nested
    @DisplayName("아티스트 프로필 수정 API")
    class UpdateArtistProfile {

        @Test
        @DisplayName("아티스트 프로필 수정에 성공한다")
        void updateArtistProfile_success() {
            // given
            Long userId = 10L;
            Long artistId = 1L;

            ArtistProfileUpdateRequest request = new ArtistProfileUpdateRequest(
                    "아이유 리브랜딩",
                    "대한민국 솔로 가수",
                    "https://example.com/new-iu.jpg"
            );

            ArtistDetailResponse detailResponse = new ArtistDetailResponse(
                    artistId,
                    "아이유 리브랜딩",
                    ArtistType.SOLO.name(),
                    ArtistStatus.ACTIVE.name(),
                    "대한민국 솔로 가수",
                    "https://example.com/new-iu.jpg",
                    LocalDateTime.of(2026, 4, 15, 10, 0, 0),
                    LocalDateTime.of(2026, 4, 15, 11, 0, 0)
            );

            when(artistService.updateArtistProfile(userId, artistId, request))
                    .thenReturn(detailResponse);

            // when
            ResponseEntity<BaseResponse<ArtistDetailResponse>> response =
                    artistController.updateArtistProfile(userId, artistId, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            verify(artistService, times(1)).updateArtistProfile(userId, artistId, request);
        }
    }

    @Nested
    @DisplayName("아티스트 삭제 API")
    class DeleteArtist {

        @Test
        @DisplayName("아티스트 삭제에 성공한다")
        void deleteArtist_success() {
            // given
            Long userId = 10L;
            Long artistId = 1L;

            doNothing().when(artistService).deleteArtist(userId, artistId);

            // when
            ResponseEntity<BaseResponse<Void>> response =
                    artistController.deleteArtist(userId, artistId);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            verify(artistService, times(1)).deleteArtist(userId, artistId);
        }
    }

    @Nested
    @DisplayName("아티스트 활성화 API")
    class ActivateArtist {

        @Test
        @DisplayName("아티스트 활성화에 성공한다")
        void activateArtist_success() {
            // given
            Long userId = 10L;
            Long artistId = 1L;

            ArtistDetailResponse detailResponse = new ArtistDetailResponse(
                    artistId,
                    "아이유",
                    ArtistType.SOLO.name(),
                    ArtistStatus.ACTIVE.name(),
                    "가수",
                    "https://example.com/iu.jpg",
                    LocalDateTime.of(2026, 4, 15, 10, 0, 0),
                    LocalDateTime.of(2026, 4, 15, 11, 0, 0)
            );

            when(artistService.activateArtist(userId, artistId))
                    .thenReturn(detailResponse);

            // when
            ResponseEntity<BaseResponse<ArtistDetailResponse>> response =
                    artistController.activateArtist(userId, artistId);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            verify(artistService, times(1)).activateArtist(userId, artistId);
        }
    }

    @Nested
    @DisplayName("아티스트 비활성화 API")
    class DeactivateArtist {

        @Test
        @DisplayName("아티스트 비활성화에 성공한다")
        void deactivateArtist_success() {
            // given
            Long userId = 10L;
            Long artistId = 1L;

            ArtistDetailResponse detailResponse = new ArtistDetailResponse(
                    artistId,
                    "아이유",
                    ArtistType.SOLO.name(),
                    ArtistStatus.INACTIVE.name(),
                    "가수",
                    "https://example.com/iu.jpg",
                    LocalDateTime.of(2026, 4, 15, 10, 0, 0),
                    LocalDateTime.of(2026, 4, 15, 11, 0, 0)
            );

            when(artistService.deactivateArtist(userId, artistId))
                    .thenReturn(detailResponse);

            // when
            ResponseEntity<BaseResponse<ArtistDetailResponse>> response =
                    artistController.deactivateArtist(userId, artistId);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            verify(artistService, times(1)).deactivateArtist(userId, artistId);
        }
    }
}