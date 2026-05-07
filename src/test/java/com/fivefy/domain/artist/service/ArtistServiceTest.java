package com.fivefy.domain.artist.service;

import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.artist.dto.request.ArtistApplicationCreateRequest;
import com.fivefy.domain.artist.dto.request.ArtistApplicationRejectRequest;
import com.fivefy.domain.artist.dto.request.ArtistProfileUpdateRequest;
import com.fivefy.domain.artist.dto.response.*;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.artist.entity.ArtistApplication;
import com.fivefy.domain.artist.enums.ArtistApplicationErrorCode;
import com.fivefy.domain.artist.enums.ArtistErrorCode;
import com.fivefy.domain.artist.enums.ArtistStatus;
import com.fivefy.domain.artist.enums.ArtistType;
import com.fivefy.domain.artist.repository.ArtistApplicationRepository;
import com.fivefy.domain.artist.repository.ArtistRepository;
import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.enums.UserErrorCode;
import com.fivefy.domain.user.enums.UserRole;
import com.fivefy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ArtistService의 비즈니스 로직을 검증하는 단위 테스트
 *
 * 아티스트 등록 신청 생성 기능 검증
 * 내 아티스트 등록 신청 목록 조회 기능 검증
 * 아티스트 등록 신청 상세 조회 기능 검증
 * 아티스트 등록 신청 목록 조회 기능 검증
 * 아티스트 등록 신청 승인 기능 검증
 * 아티스트 등록 신청 거절 기능 검증
 * 내 아티스트 목록 조회 기능 검증
 * 아티스트 상세 조회 기능 검증
 * 아티스트 프로필 수정 기능 검증
 * 아티스트 삭제 기능 검증
 * 아티스트 활성화 기능 검증
 * 아티스트 비활성화 기능 검증
 */
@ExtendWith(MockitoExtension.class)
class ArtistServiceTest {

    @Mock
    private ArtistApplicationRepository artistApplicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ArtistRepository artistRepository;

    @InjectMocks
    private ArtistService artistService;

    @Nested
    @DisplayName("아티스트 등록 신청 생성")
    class CreateArtistApplication {

        @Test
        @DisplayName("생성 성공")
        void createArtistApplication_success() {
            Long userId = 1L;

            ArtistApplicationCreateRequest request = new ArtistApplicationCreateRequest(
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/profile.jpg"
            );

            User user = mock(User.class);
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
            when(artistApplicationRepository.existsPendingApplication(
                    userId,
                    request.requestedName(),
                    request.artistType()
            )).thenReturn(false);
            when(artistApplicationRepository.existsApprovedApplication(
                    userId,
                    request.requestedName(),
                    request.artistType()
            )).thenReturn(false);

            ArtistApplication savedApplication = ArtistApplication.create(
                    userId,
                    request.requestedName(),
                    request.artistType(),
                    request.bio(),
                    request.profileImageUrl()
            );
            ReflectionTestUtils.setField(savedApplication, "id", 1L);
            ReflectionTestUtils.setField(
                    savedApplication,
                    "createdAt",
                    LocalDateTime.of(2026, 4, 14, 22, 30, 0)
            );

            when(artistApplicationRepository.saveAndFlush(any(ArtistApplication.class)))
                    .thenReturn(savedApplication);

            ArtistApplicationResponse response =
                    artistService.createArtistApplication(userId, request);

            assertThat(response.applicationId()).isEqualTo(1L);
            assertThat(response.requestedName()).isEqualTo("아이유");
            assertThat(response.artistType()).isEqualTo(ArtistType.SOLO);
            assertThat(response.status()).isEqualTo(ApplicationStatus.PENDING);
            assertThat(response.createdAt()).isEqualTo(
                    LocalDateTime.of(2026, 4, 14, 22, 30, 0)
            );
        }

        @Test
        @DisplayName("유저 없으면 실패")
        void createArtistApplication_fail_whenUserNotFound() {
            Long userId = 1L;

            ArtistApplicationCreateRequest request = new ArtistApplicationCreateRequest(
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/profile.jpg"
            );

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> artistService.createArtistApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(UserErrorCode.ERR_USER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("진행 중인 중복 신청이면 실패")
        void createArtistApplication_fail_whenPendingApplicationExists() {
            Long userId = 1L;

            ArtistApplicationCreateRequest request = new ArtistApplicationCreateRequest(
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/profile.jpg"
            );

            User user = mock(User.class);
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
            when(artistApplicationRepository.existsPendingApplication(
                    userId,
                    request.requestedName(),
                    request.artistType()
            )).thenReturn(true);

            assertThatThrownBy(() -> artistService.createArtistApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_ALREADY_EXISTS.getMessage());
        }

        @Test
        @DisplayName("승인된 중복 신청이면 실패")
        void createArtistApplication_fail_whenApprovedApplicationExists() {
            Long userId = 1L;

            ArtistApplicationCreateRequest request = new ArtistApplicationCreateRequest(
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/profile.jpg"
            );

            User user = mock(User.class);
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
            when(artistApplicationRepository.existsPendingApplication(
                    userId,
                    request.requestedName(),
                    request.artistType()
            )).thenReturn(false);
            when(artistApplicationRepository.existsApprovedApplication(
                    userId,
                    request.requestedName(),
                    request.artistType()
            )).thenReturn(true);

            assertThatThrownBy(() -> artistService.createArtistApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_ALREADY_PROCESSED.getMessage());
        }
    }

    @Nested
    @DisplayName("내 아티스트 등록 신청 목록 조회")
    class GetMyArtistApplications {

        @Test
        @DisplayName("최신순 조회 성공")
        void getMyArtistApplications_success() {
            Long userId = 1L;

            User user = mock(User.class);
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

            ArtistApplication firstApplication = ArtistApplication.create(
                    userId,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/iu.jpg"
            );
            ReflectionTestUtils.setField(firstApplication, "id", 2L);
            ReflectionTestUtils.setField(
                    firstApplication,
                    "createdAt",
                    LocalDateTime.of(2026, 4, 15, 10, 0, 0)
            );

            ArtistApplication secondApplication = ArtistApplication.create(
                    userId,
                    "아이유 밴드",
                    ArtistType.COLLABORATION,
                    "프로젝트 아티스트",
                    "https://example.com/band.jpg"
            );
            ReflectionTestUtils.setField(secondApplication, "id", 1L);
            ReflectionTestUtils.setField(
                    secondApplication,
                    "createdAt",
                    LocalDateTime.of(2026, 4, 14, 10, 0, 0)
            );

            when(artistApplicationRepository.searchMyArtistApplications(userId))
                    .thenReturn(List.of(firstApplication, secondApplication));

            List<ArtistApplicationResponse> response =
                    artistService.getMyArtistApplications(userId);

            assertThat(response).hasSize(2);
            assertThat(response.get(0).applicationId()).isEqualTo(2L);
            assertThat(response.get(0).requestedName()).isEqualTo("아이유");
            assertThat(response.get(1).applicationId()).isEqualTo(1L);
            assertThat(response.get(1).requestedName()).isEqualTo("아이유 밴드");
        }

        @Test
        @DisplayName("빈 목록 조회 성공")
        void getMyArtistApplications_success_whenEmpty() {
            Long userId = 1L;

            User user = mock(User.class);
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
            when(artistApplicationRepository.searchMyArtistApplications(userId))
                    .thenReturn(List.of());

            List<ArtistApplicationResponse> response =
                    artistService.getMyArtistApplications(userId);

            assertThat(response).isEmpty();
        }

        @Test
        @DisplayName("유저 없으면 실패")
        void getMyArtistApplications_fail_whenUserNotFound() {
            Long userId = 1L;

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> artistService.getMyArtistApplications(userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(UserErrorCode.ERR_USER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("동시 중복 신청으로 저장 충돌이 발생하면 아티스트 등록 신청 생성 실패")
        void createArtistApplication_fail_whenDuplicateSaveConflict() {
            Long userId = 1L;

            ArtistApplicationCreateRequest request = new ArtistApplicationCreateRequest(
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/profile.jpg"
            );

            User user = mock(User.class);
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
            when(artistApplicationRepository.existsPendingApplication(
                    userId,
                    request.requestedName(),
                    request.artistType()
            )).thenReturn(false);
            when(artistApplicationRepository.existsApprovedApplication(
                    userId,
                    request.requestedName(),
                    request.artistType()
            )).thenReturn(false);
            when(artistApplicationRepository.saveAndFlush(any(ArtistApplication.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate artist application"));

            assertThatThrownBy(() -> artistService.createArtistApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_ALREADY_EXISTS.getMessage());
        }
    }

    @Nested
    @DisplayName("아티스트 등록 신청 상세 조회")
    class GetArtistApplication {

        @Test
        @DisplayName("요청자 본인은 아티스트 등록 신청 상세를 조회할 수 있다")
        void getArtistApplication_requester_success() {
            // given
            Long userId = 1L;
            Long applicationId = 1L;

            ArtistApplication application = ArtistApplication.create(
                    userId,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/iu.jpg"
            );

            ReflectionTestUtils.setField(application, "id", applicationId);
            ReflectionTestUtils.setField(application, "createdAt",
                    LocalDateTime.of(2026, 4, 14, 10, 0, 0));
            ReflectionTestUtils.setField(application, "updatedAt",
                    LocalDateTime.of(2026, 4, 14, 11, 0, 0));

            User user = mock(User.class);
            when(user.getRole()).thenReturn(UserRole.USER);

            when(artistApplicationRepository.findById(applicationId))
                    .thenReturn(java.util.Optional.of(application));
            when(userRepository.findByIdAndDeletedAtIsNull(userId))
                    .thenReturn(java.util.Optional.of(user));

            // when
            ArtistApplicationDetailResponse response =
                    artistService.getArtistApplication(userId, applicationId);

            // then
            assertThat(response.applicationId()).isEqualTo(applicationId);
            assertThat(response.requesterUserId()).isEqualTo(userId);
            assertThat(response.requestedName()).isEqualTo("아이유");
            assertThat(response.artistType()).isEqualTo(ArtistType.SOLO);
            assertThat(response.bio()).isEqualTo("가수");
            assertThat(response.profileImageUrl()).isEqualTo("https://example.com/iu.jpg");
            assertThat(response.status()).isEqualTo(ApplicationStatus.PENDING);
            assertThat(response.reviewedByAdminId()).isNull();
            assertThat(response.reviewedAt()).isNull();
            assertThat(response.rejectionReason()).isNull();
            assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 4, 14, 10, 0, 0));
            assertThat(response.updatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 14, 11, 0, 0));

            verify(artistApplicationRepository, times(1)).findById(applicationId);
            verify(userRepository, times(1)).findByIdAndDeletedAtIsNull(userId);
        }

        @Test
        @DisplayName("관리자는 아티스트 등록 신청 상세를 조회할 수 있다")
        void getArtistApplication_admin_success() {
            // given
            Long userId = 99L;
            Long applicationId = 1L;

            ArtistApplication application = ArtistApplication.create(
                    1L,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/iu.jpg"
            );

            ReflectionTestUtils.setField(application, "id", applicationId);
            ReflectionTestUtils.setField(application, "createdAt",
                    LocalDateTime.of(2026, 4, 14, 10, 0, 0));
            ReflectionTestUtils.setField(application, "updatedAt",
                    LocalDateTime.of(2026, 4, 14, 11, 0, 0));

            User adminUser = mock(User.class);
            when(adminUser.getRole()).thenReturn(UserRole.ADMIN);

            when(artistApplicationRepository.findById(applicationId))
                    .thenReturn(java.util.Optional.of(application));
            when(userRepository.findByIdAndDeletedAtIsNull(userId))
                    .thenReturn(java.util.Optional.of(adminUser));

            // when
            ArtistApplicationDetailResponse response =
                    artistService.getArtistApplication(userId, applicationId);

            // then
            assertThat(response.applicationId()).isEqualTo(applicationId);
            assertThat(response.requesterUserId()).isEqualTo(1L);
            assertThat(response.requestedName()).isEqualTo("아이유");
            assertThat(response.artistType()).isEqualTo(ArtistType.SOLO);
            assertThat(response.status()).isEqualTo(ApplicationStatus.PENDING);

            verify(artistApplicationRepository, times(1)).findById(applicationId);
            verify(userRepository, times(1)).findByIdAndDeletedAtIsNull(userId);
        }

        @Test
        @DisplayName("요청자 본인도 관리자도 아니면 예외가 발생한다")
        void getArtistApplication_forbidden() {
            // given
            Long userId = 2L;
            Long applicationId = 1L;

            ArtistApplication application = ArtistApplication.create(
                    1L,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/iu.jpg"
            );

            User user = mock(User.class);
            when(user.getRole()).thenReturn(UserRole.USER);

            when(artistApplicationRepository.findById(applicationId))
                    .thenReturn(java.util.Optional.of(application));
            when(userRepository.findByIdAndDeletedAtIsNull(userId))
                    .thenReturn(java.util.Optional.of(user));

            // when & then
            assertThatThrownBy(() -> artistService.getArtistApplication(userId, applicationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_DETAIL_FORBIDDEN.getMessage());

            verify(artistApplicationRepository, times(1)).findById(applicationId);
            verify(userRepository, times(1)).findByIdAndDeletedAtIsNull(userId);
        }

        @Test
        @DisplayName("존재하지 않는 아티스트 등록 신청이면 예외가 발생한다")
        void getArtistApplication_notFound() {
            // given
            Long userId = 1L;
            Long applicationId = 1L;

            when(artistApplicationRepository.findById(applicationId))
                    .thenReturn(java.util.Optional.empty());

            // when & then
            assertThatThrownBy(() -> artistService.getArtistApplication(userId, applicationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_NOT_FOUND.getMessage());

            verify(artistApplicationRepository, times(1)).findById(applicationId);
            verify(userRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("아티스트 등록 신청 목록 조회")
    class GetArtistApplications {

        @Test
        @DisplayName("상태 조건 없이 목록 조회 성공")
        void getArtistApplications_success_withoutStatus() {
            Pageable pageable = PageRequest.of(0, 5);

            ArtistApplication firstApplication = ArtistApplication.create(
                    1L,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/iu.jpg"
            );
            ReflectionTestUtils.setField(firstApplication, "id", 1L);
            ReflectionTestUtils.setField(
                    firstApplication,
                    "createdAt",
                    LocalDateTime.of(2026, 4, 14, 10, 0, 0)
            );

            ArtistApplication secondApplication = ArtistApplication.create(
                    2L,
                    "볼빨간사춘기",
                    ArtistType.COLLABORATION,
                    "듀오",
                    "https://example.com/bol4.jpg"
            );
            ReflectionTestUtils.setField(secondApplication, "id", 2L);
            ReflectionTestUtils.setField(
                    secondApplication,
                    "createdAt",
                    LocalDateTime.of(2026, 4, 15, 10, 0, 0)
            );

            Page<ArtistApplication> page = new PageImpl<>(
                    List.of(firstApplication, secondApplication),
                    pageable,
                    2
            );

            when(artistApplicationRepository.searchArtistApplications(null, pageable))
                    .thenReturn(page);

            PageResponse<ArtistApplicationListResponse> response =
                    artistService.getArtistApplications(null, pageable);

            assertThat(response.content()).hasSize(2);
            assertThat(response.content().get(0).applicationId()).isEqualTo(1L);
            assertThat(response.content().get(0).requesterUserId()).isEqualTo(1L);
            assertThat(response.content().get(0).requestedName()).isEqualTo("아이유");
            assertThat(response.content().get(1).applicationId()).isEqualTo(2L);
            assertThat(response.page()).isEqualTo(0);
            assertThat(response.size()).isEqualTo(5);
            assertThat(response.totalElements()).isEqualTo(2);
            assertThat(response.totalPages()).isEqualTo(1);
        }

        @Test
        @DisplayName("상태 조건으로 목록 조회 성공")
        void getArtistApplications_success_withStatus() {
            Pageable pageable = PageRequest.of(0, 5);

            ArtistApplication application = ArtistApplication.create(
                    1L,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/iu.jpg"
            );
            ReflectionTestUtils.setField(application, "id", 1L);
            ReflectionTestUtils.setField(
                    application,
                    "createdAt",
                    LocalDateTime.of(2026, 4, 14, 10, 0, 0)
            );

            Page<ArtistApplication> page = new PageImpl<>(List.of(application), pageable, 1);

            when(artistApplicationRepository.searchArtistApplications(ApplicationStatus.PENDING, pageable))
                    .thenReturn(page);

            PageResponse<ArtistApplicationListResponse> response =
                    artistService.getArtistApplications(ApplicationStatus.PENDING, pageable);

            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).status()).isEqualTo(ApplicationStatus.PENDING);
            assertThat(response.totalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("조회 결과가 없으면 빈 페이지 조회 성공")
        void getArtistApplications_success_whenEmpty() {
            Pageable pageable = PageRequest.of(0, 5);

            when(artistApplicationRepository.searchArtistApplications(ApplicationStatus.REJECTED, pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            PageResponse<ArtistApplicationListResponse> response =
                    artistService.getArtistApplications(ApplicationStatus.REJECTED, pageable);

            assertThat(response.content()).isEmpty();
            assertThat(response.totalElements()).isZero();
            assertThat(response.totalPages()).isZero();
        }
    }

    @Nested
    @DisplayName("아티스트 등록 신청 승인")
    class ApproveArtistApplication {

        @Test
        @DisplayName("승인 성공")
        void approveArtistApplication_success() {
            Long adminId = 1L;
            Long applicationId = 10L;

            ArtistApplication application = ArtistApplication.create(
                    2L,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/profile.jpg"
            );
            ReflectionTestUtils.setField(application, "id", applicationId);

            Artist savedArtist = Artist.create(
                    application.getRequesterUserId(),
                    application.getRequestedName(),
                    application.getArtistType(),
                    application.getBio(),
                    application.getProfileImageUrl()
            );
            ReflectionTestUtils.setField(savedArtist, "id", 100L);

            when(artistApplicationRepository.findByIdForUpdate(applicationId))
                    .thenReturn(Optional.of(application));
            when(artistRepository.save(any(Artist.class)))
                    .thenReturn(savedArtist);

            ArtistApplicationApproveResponse response =
                    artistService.approveArtistApplication(adminId, applicationId);

            assertThat(response.applicationId()).isEqualTo(applicationId);
            assertThat(response.artistId()).isEqualTo(100L);
            assertThat(response.artistType()).isEqualTo(ArtistType.SOLO);
            assertThat(response.status()).isEqualTo(ApplicationStatus.APPROVED);
            assertThat(response.reviewedByAdminId()).isEqualTo(adminId);
            assertThat(response.reviewedAt()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 신청이면 승인 실패")
        void approveArtistApplication_fail_whenNotFound() {
            Long adminId = 1L;
            Long applicationId = 10L;

            when(artistApplicationRepository.findByIdForUpdate(applicationId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> artistService.approveArtistApplication(adminId, applicationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("이미 처리된 신청이면 승인 실패")
        void approveArtistApplication_fail_whenAlreadyProcessed() {
            Long adminId = 1L;
            Long applicationId = 10L;

            ArtistApplication application = ArtistApplication.create(
                    2L,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/profile.jpg"
            );
            ReflectionTestUtils.setField(application, "id", applicationId);
            application.approve(adminId);

            when(artistApplicationRepository.findByIdForUpdate(applicationId))
                    .thenReturn(Optional.of(application));

            assertThatThrownBy(() -> artistService.approveArtistApplication(adminId, applicationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_ALREADY_PROCESSED.getMessage());
        }
    }

    @Nested
    @DisplayName("아티스트 등록 신청 거절")
    class RejectArtistApplication {

        @Test
        @DisplayName("거절 성공")
        void rejectArtistApplication_success() {
            Long adminId = 1L;
            Long applicationId = 10L;

            ArtistApplicationRejectRequest request =
                    new ArtistApplicationRejectRequest("정보 부족");

            ArtistApplication application = ArtistApplication.create(
                    2L,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/profile.jpg"
            );
            ReflectionTestUtils.setField(application, "id", applicationId);

            when(artistApplicationRepository.findByIdForUpdate(applicationId))
                    .thenReturn(Optional.of(application));

            ArtistApplicationRejectResponse response =
                    artistService.rejectArtistApplication(adminId, applicationId, request);

            assertThat(response.applicationId()).isEqualTo(applicationId);
            assertThat(response.artistType()).isEqualTo(ArtistType.SOLO);
            assertThat(response.status()).isEqualTo(ApplicationStatus.REJECTED);
            assertThat(response.reviewedByAdminId()).isEqualTo(adminId);
            assertThat(response.reviewedAt()).isNotNull();
            assertThat(response.rejectionReason()).isEqualTo("정보 부족");
        }

        @Test
        @DisplayName("존재하지 않는 신청이면 거절 실패")
        void rejectArtistApplication_fail_whenNotFound() {
            Long adminId = 1L;
            Long applicationId = 10L;

            ArtistApplicationRejectRequest request =
                    new ArtistApplicationRejectRequest("정보 부족");

            when(artistApplicationRepository.findByIdForUpdate(applicationId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> artistService.rejectArtistApplication(adminId, applicationId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("이미 처리된 신청이면 거절 실패")
        void rejectArtistApplication_fail_whenAlreadyProcessed() {
            Long adminId = 1L;
            Long applicationId = 10L;

            ArtistApplicationRejectRequest request =
                    new ArtistApplicationRejectRequest("정보 부족");

            ArtistApplication application = ArtistApplication.create(
                    2L,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/profile.jpg"
            );
            ReflectionTestUtils.setField(application, "id", applicationId);
            application.reject(adminId, "기존 거절 사유");

            when(artistApplicationRepository.findByIdForUpdate(applicationId))
                    .thenReturn(Optional.of(application));

            assertThatThrownBy(() -> artistService.rejectArtistApplication(adminId, applicationId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_ALREADY_PROCESSED.getMessage());
        }
    }

    @Nested
    @DisplayName("내 아티스트 목록 조회")
    class GetMyArtists {

        @Test
        @DisplayName("내 아티스트 목록 조회에 성공한다")
        void getMyArtists_success() {
            // given
            Long userId = 1L;

            User user = mock(User.class);
            when(userRepository.findByIdAndDeletedAtIsNull(userId))
                    .thenReturn(java.util.Optional.of(user));

            Artist firstArtist = Artist.create(
                    userId,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/iu.jpg"
            );
            Artist secondArtist = Artist.create(
                    userId,
                    "아이유 밴드",
                    ArtistType.COLLABORATION,
                    "프로젝트 아티스트",
                    "https://example.com/band.jpg"
            );

            ReflectionTestUtils.setField(firstArtist, "id", 2L);
            ReflectionTestUtils.setField(firstArtist, "createdAt",
                    LocalDateTime.of(2026, 4, 15, 10, 0, 0));
            ReflectionTestUtils.setField(firstArtist, "updatedAt",
                    LocalDateTime.of(2026, 4, 15, 11, 0, 0));

            ReflectionTestUtils.setField(secondArtist, "id", 1L);
            ReflectionTestUtils.setField(secondArtist, "createdAt",
                    LocalDateTime.of(2026, 4, 14, 10, 0, 0));
            ReflectionTestUtils.setField(secondArtist, "updatedAt",
                    LocalDateTime.of(2026, 4, 14, 11, 0, 0));

            when(artistRepository.findMyArtists(userId))
                    .thenReturn(List.of(firstArtist, secondArtist));

            // when
            List<MyArtistResponse> response = artistService.getMyArtists(userId);

            // then
            assertThat(response).hasSize(2);
            assertThat(response.get(0).artistId()).isEqualTo(2L);
            assertThat(response.get(0).name()).isEqualTo("아이유");
            assertThat(response.get(0).artistType()).isEqualTo(ArtistType.SOLO);
            assertThat(response.get(0).bio()).isEqualTo("가수");
            assertThat(response.get(0).profileImageUrl()).isEqualTo("https://example.com/iu.jpg");
            assertThat(response.get(0).createdAt()).isEqualTo(LocalDateTime.of(2026, 4, 15, 10, 0, 0));
            assertThat(response.get(0).updatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 15, 11, 0, 0));

            assertThat(response.get(1).artistId()).isEqualTo(1L);
            assertThat(response.get(1).name()).isEqualTo("아이유 밴드");
            assertThat(response.get(1).artistType()).isEqualTo(ArtistType.COLLABORATION);
            assertThat(response.get(1).bio()).isEqualTo("프로젝트 아티스트");
            assertThat(response.get(1).profileImageUrl()).isEqualTo("https://example.com/band.jpg");
            assertThat(response.get(1).createdAt()).isEqualTo(LocalDateTime.of(2026, 4, 14, 10, 0, 0));
            assertThat(response.get(1).updatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 14, 11, 0, 0));

            verify(userRepository, times(1)).findByIdAndDeletedAtIsNull(userId);
            verify(artistRepository, times(1)).findMyArtists(userId);
        }

        @Test
        @DisplayName("소유한 아티스트가 없으면 빈 목록을 반환한다")
        void getMyArtists_empty() {
            // given
            Long userId = 1L;

            User user = mock(User.class);
            when(userRepository.findByIdAndDeletedAtIsNull(userId))
                    .thenReturn(java.util.Optional.of(user));

            when(artistRepository.findMyArtists(userId))
                    .thenReturn(List.of());

            // when
            List<MyArtistResponse> response = artistService.getMyArtists(userId);

            // then
            assertThat(response).isEmpty();

            verify(userRepository, times(1)).findByIdAndDeletedAtIsNull(userId);
            verify(artistRepository, times(1)).findMyArtists(userId);
        }
    }

    @Nested
    @DisplayName("아티스트 상세 조회")
    class GetArtist {

        @Test
        @DisplayName("아티스트 상세 조회에 성공한다")
        void getArtist_success() {
            // given
            Long artistId = 1L;

            Artist artist = Artist.create(
                    1L,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/iu.jpg"
            );

            ReflectionTestUtils.setField(artist, "id", artistId);
            ReflectionTestUtils.setField(artist, "createdAt",
                    LocalDateTime.of(2026, 4, 15, 10, 0, 0));
            ReflectionTestUtils.setField(artist, "updatedAt",
                    LocalDateTime.of(2026, 4, 15, 11, 0, 0));

            when(artistRepository.findById(artistId))
                    .thenReturn(java.util.Optional.of(artist));

            // when
            ArtistDetailResponse response = artistService.getArtist(artistId);

            // then
            assertThat(response.artistId()).isEqualTo(artistId);
            assertThat(response.name()).isEqualTo("아이유");
            assertThat(response.artistType()).isEqualTo(ArtistType.SOLO);
            assertThat(response.status()).isEqualTo(ArtistStatus.ACTIVE);

            verify(artistRepository, times(1)).findById(artistId);
        }

        @Test
        @DisplayName("존재하지 않는 아티스트면 예외가 발생한다")
        void getArtist_notFound() {
            // given
            Long artistId = 1L;

            when(artistRepository.findById(artistId))
                    .thenReturn(java.util.Optional.empty());

            // when & then
            assertThatThrownBy(() -> artistService.getArtist(artistId))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("삭제된 아티스트는 상세 조회할 수 없다")
        void getArtist_fail_whenDeletedArtist() {
            // given
            Long artistId = 1L;

            Artist artist = Artist.create(
                    1L,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/iu.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);
            ReflectionTestUtils.setField(artist, "deletedAt",
                    LocalDateTime.of(2026, 4, 15, 12, 0, 0));

            when(artistRepository.findById(artistId))
                    .thenReturn(java.util.Optional.of(artist));

            // when & then
            assertThatThrownBy(() -> artistService.getArtist(artistId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_ARTIST_NOT_FOUND.getMessage());

            verify(artistRepository, times(1)).findById(artistId);
        }
    }

    @Nested
    @DisplayName("아티스트 프로필 수정")
    class UpdateArtistProfile {

        @Test
        @DisplayName("아티스트 소유자는 프로필 수정에 성공한다")
        void updateArtistProfile_success() {
            // given
            Long userId = 10L;
            Long artistId = 1L;
            ArtistProfileUpdateRequest request = new ArtistProfileUpdateRequest(
                    "아이유 리브랜딩",
                    "대한민국 솔로 가수",
                    "https://example.com/new-iu.jpg"
            );

            Artist artist = Artist.create(
                    userId,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/iu.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);
            ReflectionTestUtils.setField(artist, "createdAt",
                    LocalDateTime.of(2026, 4, 15, 10, 0, 0));
            ReflectionTestUtils.setField(artist, "updatedAt",
                    LocalDateTime.of(2026, 4, 15, 11, 0, 0));

            when(artistRepository.findById(artistId))
                    .thenReturn(java.util.Optional.of(artist));

            // when
            ArtistDetailResponse response =
                    artistService.updateArtistProfile(userId, artistId, request);

            // then
            assertThat(response.artistId()).isEqualTo(artistId);
            assertThat(response.name()).isEqualTo("아이유 리브랜딩");
            assertThat(response.artistType()).isEqualTo(ArtistType.SOLO);
            assertThat(response.bio()).isEqualTo("대한민국 솔로 가수");
            assertThat(response.status()).isEqualTo(ArtistStatus.ACTIVE);
            assertThat(response.profileImageUrl()).isEqualTo("https://example.com/new-iu.jpg");
            assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 4, 15, 10, 0, 0));

            verify(artistRepository, times(1)).findById(artistId);
        }

        @Test
        @DisplayName("삭제된 아티스트는 프로필 수정할 수 없다")
        void updateArtistProfile_fail_whenDeletedArtist() {
            // given
            Long userId = 10L;
            Long artistId = 1L;
            ArtistProfileUpdateRequest request = new ArtistProfileUpdateRequest(
                    "아이유 리브랜딩",
                    "대한민국 솔로 가수",
                    "https://example.com/new-iu.jpg"
            );

            Artist artist = Artist.create(
                    userId,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/iu.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);
            ReflectionTestUtils.setField(artist, "deletedAt",
                    LocalDateTime.of(2026, 4, 15, 12, 0, 0));

            when(artistRepository.findById(artistId))
                    .thenReturn(java.util.Optional.of(artist));

            // when & then
            assertThatThrownBy(() -> artistService.updateArtistProfile(userId, artistId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_ARTIST_NOT_FOUND.getMessage());

            verify(artistRepository, times(1)).findById(artistId);
        }

        @Test
        @DisplayName("아티스트 소유자가 아니면 프로필 수정할 수 없다")
        void updateArtistProfile_fail_whenNotOwner() {
            // given
            Long userId = 99L;
            Long artistId = 1L;
            ArtistProfileUpdateRequest request = new ArtistProfileUpdateRequest(
                    "아이유 리브랜딩",
                    "대한민국 솔로 가수",
                    "https://example.com/new-iu.jpg"
            );

            Artist artist = Artist.create(
                    10L,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/iu.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);

            when(artistRepository.findById(artistId))
                    .thenReturn(java.util.Optional.of(artist));

            // when & then
            assertThatThrownBy(() -> artistService.updateArtistProfile(userId, artistId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_FORBIDDEN_ARTIST_ACCESS.getMessage());

            verify(artistRepository, times(1)).findById(artistId);
        }

        @Test
        @DisplayName("비활성화된 아티스트도 프로필 수정에 성공한다")
        void updateArtistProfile_success_whenInactiveArtist() {
            // given
            Long artistId = 1L;
            Long userId = 10L;
            ArtistProfileUpdateRequest request = new ArtistProfileUpdateRequest(
                    "아이유 리브랜딩",
                    "대한민국 솔로 가수",
                    "https://example.com/new-iu.jpg"
            );

            Artist artist = Artist.create(
                    userId,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/iu.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);
            ReflectionTestUtils.setField(artist, "createdAt",
                    LocalDateTime.of(2026, 4, 15, 10, 0, 0));
            ReflectionTestUtils.setField(artist, "updatedAt",
                    LocalDateTime.of(2026, 4, 15, 11, 0, 0));

            artist.deactivate();

            when(artistRepository.findById(artistId))
                    .thenReturn(java.util.Optional.of(artist));

            // when
            ArtistDetailResponse response =
                    artistService.updateArtistProfile(userId, artistId, request);

            // then
            assertThat(response.artistId()).isEqualTo(artistId);
            assertThat(response.name()).isEqualTo("아이유 리브랜딩");
            assertThat(response.artistType()).isEqualTo(ArtistType.SOLO);
            assertThat(response.status()).isEqualTo(ArtistStatus.INACTIVE);
            assertThat(response.bio()).isEqualTo("대한민국 솔로 가수");
            assertThat(response.profileImageUrl()).isEqualTo("https://example.com/new-iu.jpg");

            verify(artistRepository, times(1)).findById(artistId);
        }

        @Test
        @DisplayName("존재하지 않는 아티스트는 프로필 수정할 수 없다")
        void updateArtistProfile_fail_whenArtistNotFound() {
            // given
            Long artistId = 1L;
            Long userId = 10L;
            ArtistProfileUpdateRequest request = new ArtistProfileUpdateRequest(
                    "아이유 리브랜딩",
                    "대한민국 솔로 가수",
                    "https://example.com/new-iu.jpg"
            );

            when(artistRepository.findById(artistId))
                    .thenReturn(java.util.Optional.empty());

            // when & then
            assertThatThrownBy(() -> artistService.updateArtistProfile(userId, artistId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_ARTIST_NOT_FOUND.getMessage());

            verify(artistRepository, times(1)).findById(artistId);
        }
    }

    @Nested
    @DisplayName("아티스트 삭제")
    class DeleteArtist {

        @Test
        @DisplayName("삭제 성공")
        void deleteArtist_success() {
            Long userId = 10L;
            Long artistId = 1L;

            Artist artist = Artist.create(
                    userId,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/iu.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);

            when(artistRepository.findById(artistId))
                    .thenReturn(Optional.of(artist));

            artistService.deleteArtist(userId, artistId);

            assertThat(artist.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 아티스트면 삭제 실패")
        void deleteArtist_fail_whenArtistNotFound() {
            Long userId = 10L;
            Long artistId = 1L;

            when(artistRepository.findById(artistId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> artistService.deleteArtist(userId, artistId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_ARTIST_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("이미 삭제된 아티스트면 삭제 실패")
        void deleteArtist_fail_whenAlreadyDeleted() {
            Long userId = 10L;
            Long artistId = 1L;

            Artist artist = Artist.create(
                    userId,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/iu.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);
            ReflectionTestUtils.setField(
                    artist,
                    "deletedAt",
                    LocalDateTime.of(2026, 4, 15, 20, 0, 0)
            );

            when(artistRepository.findById(artistId))
                    .thenReturn(Optional.of(artist));

            assertThatThrownBy(() -> artistService.deleteArtist(userId, artistId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_ARTIST_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("소유자가 아니면 삭제 실패")
        void deleteArtist_fail_whenNotOwner() {
            Long userId = 99L;
            Long artistId = 1L;

            Artist artist = Artist.create(
                    10L,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/iu.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);

            when(artistRepository.findById(artistId))
                    .thenReturn(Optional.of(artist));

            assertThatThrownBy(() -> artistService.deleteArtist(userId, artistId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_FORBIDDEN_ARTIST_ACCESS.getMessage());
        }
    }

    @Nested
    @DisplayName("아티스트 활성화")
    class ActivateArtist {

        @Test
        @DisplayName("비활성화된 아티스트는 활성화에 성공한다")
        void activateArtist_success() {
            // given
            Long userId = 10L;
            Long artistId = 1L;

            Artist artist = Artist.create(
                    userId,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/iu.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);
            ReflectionTestUtils.setField(artist, "createdAt",
                    LocalDateTime.of(2026, 4, 15, 10, 0, 0));
            ReflectionTestUtils.setField(artist, "updatedAt",
                    LocalDateTime.of(2026, 4, 15, 11, 0, 0));
            artist.deactivate();

            when(artistRepository.findById(artistId))
                    .thenReturn(java.util.Optional.of(artist));

            // when
            ArtistDetailResponse response = artistService.activateArtist(userId, artistId);

            // then
            assertThat(response.artistId()).isEqualTo(artistId);
            assertThat(response.name()).isEqualTo("아이유");
            assertThat(response.artistType()).isEqualTo(ArtistType.SOLO);
            assertThat(response.status()).isEqualTo(ArtistStatus.ACTIVE);
            assertThat(response.bio()).isEqualTo("가수");
            assertThat(response.profileImageUrl()).isEqualTo("https://example.com/iu.jpg");
            assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 4, 15, 10, 0, 0));
            assertThat(response.updatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 15, 11, 0, 0));
            assertThat(artist.getStatus()).isEqualTo(ArtistStatus.ACTIVE);

            verify(artistRepository, times(1)).findById(artistId);
        }

        @Test
        @DisplayName("존재하지 않는 아티스트는 활성화할 수 없다")
        void activateArtist_fail_whenArtistNotFound() {
            // given
            Long userId = 10L;
            Long artistId = 1L;

            when(artistRepository.findById(artistId))
                    .thenReturn(java.util.Optional.empty());

            // when & then
            assertThatThrownBy(() -> artistService.activateArtist(userId, artistId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_ARTIST_NOT_FOUND.getMessage());

            verify(artistRepository, times(1)).findById(artistId);
        }

        @Test
        @DisplayName("이미 활성화된 아티스트는 다시 활성화할 수 없다")
        void activateArtist_fail_whenAlreadyActive() {
            // given
            Long userId = 10L;
            Long artistId = 1L;

            Artist artist = Artist.create(
                    userId,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/iu.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);

            when(artistRepository.findById(artistId))
                    .thenReturn(java.util.Optional.of(artist));

            // when & then
            assertThatThrownBy(() -> artistService.activateArtist(userId, artistId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_ARTIST_ALREADY_ACTIVATED.getMessage());

            verify(artistRepository, times(1)).findById(artistId);
        }

        @Test
        @DisplayName("아티스트 소유자가 아니면 활성화할 수 없다")
        void activateArtist_fail_whenNotOwner() {
            // given
            Long userId = 99L;
            Long artistId = 1L;

            Artist artist = Artist.create(
                    10L,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/iu.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);
            artist.deactivate();

            when(artistRepository.findById(artistId))
                    .thenReturn(java.util.Optional.of(artist));

            // when & then
            assertThatThrownBy(() -> artistService.activateArtist(userId, artistId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_FORBIDDEN_ARTIST_ACCESS.getMessage());

            verify(artistRepository, times(1)).findById(artistId);
        }
    }

    @Nested
    @DisplayName("아티스트 비활성화")
    class DeactivateArtist {

        @Test
        @DisplayName("활성화된 아티스트는 비활성화에 성공한다")
        void deactivateArtist_success() {
            // given
            Long userId = 10L;
            Long artistId = 1L;

            Artist artist = Artist.create(
                    userId,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/iu.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);

            when(artistRepository.findById(artistId))
                    .thenReturn(java.util.Optional.of(artist));

            // when
            ArtistDetailResponse response = artistService.deactivateArtist(userId, artistId);

            // then
            assertThat(response.artistId()).isEqualTo(artistId);
            assertThat(response.name()).isEqualTo("아이유");
            assertThat(response.artistType()).isEqualTo(ArtistType.SOLO);
            assertThat(response.status()).isEqualTo(ArtistStatus.INACTIVE);
            assertThat(response.bio()).isEqualTo("가수");
            assertThat(response.profileImageUrl()).isEqualTo("https://example.com/iu.jpg");
            assertThat(artist.getStatus()).isEqualTo(ArtistStatus.INACTIVE);

            verify(artistRepository, times(1)).findById(artistId);
        }

        @Test
        @DisplayName("존재하지 않는 아티스트는 비활성화할 수 없다")
        void deactivateArtist_fail_whenNotFound() {
            // given
            Long userId = 10L;
            Long artistId = 1L;

            when(artistRepository.findById(artistId))
                    .thenReturn(java.util.Optional.empty());

            // when & then
            assertThatThrownBy(() -> artistService.deactivateArtist(userId, artistId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_ARTIST_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("이미 비활성화된 아티스트는 다시 비활성화할 수 없다")
        void deactivateArtist_fail_whenAlreadyInactive() {
            // given
            Long userId = 10L;
            Long artistId = 1L;

            Artist artist = Artist.create(
                    userId,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/iu.jpg"
            );
            artist.deactivate();
            ReflectionTestUtils.setField(artist, "id", artistId);

            when(artistRepository.findById(artistId))
                    .thenReturn(java.util.Optional.of(artist));

            // when & then
            assertThatThrownBy(() -> artistService.deactivateArtist(userId, artistId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_ARTIST_ALREADY_INACTIVE.getMessage());
        }

        @Test
        @DisplayName("아티스트 소유자가 아니면 비활성화할 수 없다")
        void deactivateArtist_fail_whenNotOwner() {
            // given
            Long userId = 99L;
            Long artistId = 1L;

            Artist artist = Artist.create(
                    10L,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/iu.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);

            when(artistRepository.findById(artistId))
                    .thenReturn(java.util.Optional.of(artist));

            // when & then
            assertThatThrownBy(() -> artistService.deactivateArtist(userId, artistId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_FORBIDDEN_ARTIST_ACCESS.getMessage());
        }
    }
}