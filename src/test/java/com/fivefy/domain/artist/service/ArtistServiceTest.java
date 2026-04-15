package com.fivefy.domain.artist.service;

import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.artist.dto.request.ArtistApplicationCreateRequest;
import com.fivefy.domain.artist.enums.ArtistType;
import com.fivefy.domain.artist.dto.request.ArtistApplicationRejectRequest;
import com.fivefy.domain.artist.dto.response.*;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.artist.entity.ArtistApplication;
import com.fivefy.domain.artist.enums.ArtistApplicationErrorCode;
import com.fivefy.domain.artist.repository.ArtistApplicationRepository;
import com.fivefy.domain.artist.repository.ArtistRepository;
import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.enums.UserRole;
import com.fivefy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static com.fivefy.common.enums.ApplicationStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ArtistService의 비즈니스 로직을 검증하는 단위 테스트
 *
 * 아티스트 등록 요청 생성 기능을 검증한다.
 * 내 아티스트 등록 요청 목록 조회 기능을 검증한다.
 * 관리자용 아티스트 등록 요청 목록 조회 기능을 검증한다.
 * 아티스트 등록 요청 상세 조회 기능을 검증한다.
 * 아티스트 등록 요청 승인 기능을 검증한다.
 * 아티스트 등록 요청 거절 기능을 검증한다.
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
    @DisplayName("아티스트 등록 요청 생성")
    class CreateArtistApplication {

        @Test
        @DisplayName("아티스트 등록 요청 생성에 성공한다")
        void createArtistApplication_success() {
            // given
            Long userId = 1L;
            ArtistApplicationCreateRequest request = new ArtistApplicationCreateRequest(
                    "아이유",
                    "가수",
                    "https://example.com/profile.jpg",
                    ArtistType.SOLO
            );

            User user = mock(User.class);
            when(userRepository.findById(userId))
                    .thenReturn(java.util.Optional.of(user));

            ArtistApplication savedApplication = ArtistApplication.create(
                    userId,
                    request.requestedName(),
                    request.bio(),
                    request.profileImageUrl(),
                    request.artistType()
            );

            // 단위 테스트에서는 JPA auditing이 동작하지 않으므로 createdAt을 직접 주입한다.
            ReflectionTestUtils.setField(savedApplication, "id", 1L);
            ReflectionTestUtils.setField(savedApplication, "createdAt",
                    LocalDateTime.of(2026, 4, 14, 22, 30, 0));

            when(artistApplicationRepository.existsActiveApplication(userId, request.requestedName(), request.artistType()))
                    .thenReturn(false);

            when(artistApplicationRepository.save(any(ArtistApplication.class)))
                    .thenReturn(savedApplication);

            // when
            ArtistApplicationResponse response =
                    artistService.createArtistApplication(userId, request);

            // then
            assertThat(response.applicationId()).isEqualTo(1L);
            assertThat(response.requestedName()).isEqualTo("아이유");
            assertThat(response.artistType()).isEqualTo(ArtistType.SOLO.name());
            assertThat(response.status()).isEqualTo(PENDING.name());
            assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 4, 14, 22, 30, 0));

            verify(artistApplicationRepository, times(1))
                    .save(any(ArtistApplication.class));
            verify(artistApplicationRepository, times(1))
                    .existsActiveApplication(userId, request.requestedName(), request.artistType());
            verify(userRepository, times(1)).findById(userId);
        }

        @Test
        @DisplayName("같은 이름의 진행 중이거나 승인된 요청이 있으면 생성에 실패한다")
        void createArtistApplication_fail_whenActiveApplicationExists() {
            // given
            Long userId = 1L;
            ArtistApplicationCreateRequest request = new ArtistApplicationCreateRequest(
                    "아이유",
                    "가수",
                    "https://example.com/profile.jpg",
                    ArtistType.SOLO
            );

            User user = mock(User.class);
            when(userRepository.findById(userId))
                    .thenReturn(java.util.Optional.of(user));

            when(artistApplicationRepository.existsActiveApplication(userId, request.requestedName(), request.artistType()))
                    .thenReturn(true);

            // when & then
            assertThatThrownBy(() -> artistService.createArtistApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_ALREADY_EXISTS.getMessage());

            verify(artistApplicationRepository, times(1))
                    .existsActiveApplication(userId, request.requestedName(), request.artistType());
            verify(userRepository, times(1)).findById(userId);
            verify(artistApplicationRepository, never()).save(any(ArtistApplication.class));
        }
    }

    @Nested
    @DisplayName("내 아티스트 등록 요청 목록 조회")
    class GetMyArtistApplications {

        @Test
        @DisplayName("내 아티스트 등록 요청 목록을 최신순으로 조회한다")
        void getMyArtistApplications_success() {
            // given
            Long userId = 1L;

            User user = mock(User.class);
            when(userRepository.findById(userId))
                    .thenReturn(java.util.Optional.of(user));

            ArtistApplication firstApplication = ArtistApplication.create(
                    userId,
                    "아이유",
                    "가수",
                    "https://example.com/iu.jpg",
                    ArtistType.SOLO
            );
            ArtistApplication secondApplication = ArtistApplication.create(
                    userId,
                    "아이유 밴드",
                    "프로젝트 아티스트",
                    "https://example.com/band.jpg",
                    ArtistType.COLLABORATION
            );

            ReflectionTestUtils.setField(firstApplication, "id", 2L);
            ReflectionTestUtils.setField(firstApplication, "createdAt",
                    LocalDateTime.of(2026, 4, 15, 10, 0, 0));

            ReflectionTestUtils.setField(secondApplication, "id", 1L);
            ReflectionTestUtils.setField(secondApplication, "createdAt",
                    LocalDateTime.of(2026, 4, 14, 10, 0, 0));

            when(artistApplicationRepository.findAllByRequesterUserIdOrderByCreatedAtDesc(userId))
                    .thenReturn(List.of(firstApplication, secondApplication));

            // when
            List<ArtistApplicationResponse> response = artistService.getMyArtistApplications(userId);

            // then
            assertThat(response).hasSize(2);
            assertThat(response.get(0).applicationId()).isEqualTo(2L);
            assertThat(response.get(0).requestedName()).isEqualTo("아이유");
            assertThat(response.get(0).artistType()).isEqualTo(ArtistType.SOLO.name());
            assertThat(response.get(0).status()).isEqualTo("PENDING");

            assertThat(response.get(1).applicationId()).isEqualTo(1L);
            assertThat(response.get(1).requestedName()).isEqualTo("아이유 밴드");
            assertThat(response.get(1).artistType()).isEqualTo(ArtistType.COLLABORATION.name());

            verify(artistApplicationRepository, times(1))
                    .findAllByRequesterUserIdOrderByCreatedAtDesc(userId);
            verify(userRepository, times(1)).findById(userId);
        }

        @Test
        @DisplayName("내 아티스트 등록 요청이 없으면 빈 목록을 반환한다")
        void getMyArtistApplications_empty() {
            // given
            Long userId = 1L;

            User user = mock(User.class);
            when(userRepository.findById(userId))
                    .thenReturn(java.util.Optional.of(user));

            when(artistApplicationRepository.findAllByRequesterUserIdOrderByCreatedAtDesc(userId))
                    .thenReturn(List.of());

            // when
            List<ArtistApplicationResponse> response = artistService.getMyArtistApplications(userId);

            // then
            assertThat(response).isEmpty();

            verify(artistApplicationRepository, times(1))
                    .findAllByRequesterUserIdOrderByCreatedAtDesc(userId);
            verify(userRepository, times(1)).findById(userId);
        }
    }

    @Nested
    @DisplayName("관리자용 아티스트 등록 요청 목록 조회")
    class GetArtistApplications {

        @Test
        @DisplayName("관리자는 아티스트 등록 요청 목록을 오래된 순으로 조회한다")
        void getArtistApplications_success() {
            // given
            Pageable pageable = PageRequest.of(
                    0,
                    5,
                    Sort.by(Sort.Direction.ASC, "createdAt")
            );

            ArtistApplication firstApplication = ArtistApplication.create(
                    1L,
                    "아이유",
                    "가수",
                    "https://example.com/iu.jpg",
                    ArtistType.SOLO
            );
            ArtistApplication secondApplication = ArtistApplication.create(
                    2L,
                    "볼빨간사춘기",
                    "듀오",
                    "https://example.com/bol4.jpg",
                    ArtistType.COLLABORATION
            );

            // 오래된 요청이 먼저 조회되도록 createdAt과 id를 직접 주입한다.
            ReflectionTestUtils.setField(firstApplication, "id", 1L);
            ReflectionTestUtils.setField(firstApplication, "createdAt",
                    LocalDateTime.of(2026, 4, 14, 10, 0, 0));

            ReflectionTestUtils.setField(secondApplication, "id", 2L);
            ReflectionTestUtils.setField(secondApplication, "createdAt",
                    LocalDateTime.of(2026, 4, 15, 10, 0, 0));

            Page<ArtistApplication> page = new PageImpl<>(
                    List.of(firstApplication, secondApplication),
                    pageable,
                    2
            );

            // when
            when(artistApplicationRepository.searchArtistApplications(null, pageable))
                    .thenReturn(page);

            PageResponse<ArtistApplicationListResponse> response =
                    artistService.getArtistApplications(null, pageable);

            // then
            assertThat(response.content()).hasSize(2);
            assertThat(response.page()).isEqualTo(0);
            assertThat(response.size()).isEqualTo(5);
            assertThat(response.totalElements()).isEqualTo(2);
            assertThat(response.totalPages()).isEqualTo(1);

            assertThat(response.content().get(0).applicationId()).isEqualTo(1L);
            assertThat(response.content().get(0).requesterUserId()).isEqualTo(1L);
            assertThat(response.content().get(0).requestedName()).isEqualTo("아이유");
            assertThat(response.content().get(0).artistType()).isEqualTo(ArtistType.SOLO.name());
            assertThat(response.content().get(0).status()).isEqualTo("PENDING");
            assertThat(response.content().get(0).createdAt()).isEqualTo(LocalDateTime.of(2026, 4, 14, 10, 0, 0));

            assertThat(response.content().get(1).applicationId()).isEqualTo(2L);
            assertThat(response.content().get(1).requesterUserId()).isEqualTo(2L);
            assertThat(response.content().get(1).requestedName()).isEqualTo("볼빨간사춘기");
            assertThat(response.content().get(1).artistType()).isEqualTo(ArtistType.COLLABORATION.name());
            assertThat(response.content().get(1).status()).isEqualTo("PENDING");
            assertThat(response.content().get(1).createdAt()).isEqualTo(LocalDateTime.of(2026, 4, 15, 10, 0, 0));

            verify(artistApplicationRepository, times(1))
                    .searchArtistApplications(null, pageable);
        }

        @Test
        @DisplayName("등록 요청이 없으면 빈 페이지를 반환한다")
        void getArtistApplications_empty() {
            // given
            Pageable pageable = PageRequest.of(
                    0,
                    5,
                    Sort.by(Sort.Direction.ASC, "createdAt")
            );

            Page<ArtistApplication> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(artistApplicationRepository.searchArtistApplications(null, pageable))
                    .thenReturn(emptyPage);

            // when
            PageResponse<ArtistApplicationListResponse> response =
                    artistService.getArtistApplications(null, pageable);

            // then
            assertThat(response.content()).isEmpty();
            assertThat(response.page()).isEqualTo(0);
            assertThat(response.size()).isEqualTo(5);
            assertThat(response.totalElements()).isZero();
            assertThat(response.totalPages()).isZero();

            verify(artistApplicationRepository, times(1))
                    .searchArtistApplications(null, pageable);
        }

        @Test
        @DisplayName("관리자는 상태 조건으로 아티스트 등록 요청 목록을 조회한다")
        void getArtistApplications_withStatus_success() {
            // given
            Pageable pageable = PageRequest.of(
                    0,
                    5,
                    Sort.by(Sort.Direction.ASC, "createdAt")
            );

            ArtistApplication application = ArtistApplication.create(
                    1L,
                    "아이유",
                    "가수",
                    "https://example.com/iu.jpg",
                    ArtistType.SOLO
            );

            ReflectionTestUtils.setField(application, "id", 1L);
            ReflectionTestUtils.setField(application, "createdAt",
                    LocalDateTime.of(2026, 4, 14, 10, 0, 0));

            Page<ArtistApplication> page = new PageImpl<>(List.of(application), pageable, 1);

            when(artistApplicationRepository.searchArtistApplications(ApplicationStatus.PENDING, pageable))
                    .thenReturn(page);

            // when
            PageResponse<ArtistApplicationListResponse> response =
                    artistService.getArtistApplications(ApplicationStatus.PENDING, pageable);

            // then
            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).status()).isEqualTo("PENDING");
            assertThat(response.content().get(0).artistType()).isEqualTo(ArtistType.SOLO.name());

            verify(artistApplicationRepository, times(1))
                    .searchArtistApplications(ApplicationStatus.PENDING, pageable);
        }
    }

    @Nested
    @DisplayName("아티스트 등록 요청 상세 조회")
    class GetArtistApplication {

        @Test
        @DisplayName("요청자 본인은 아티스트 등록 요청 상세를 조회할 수 있다")
        void getArtistApplication_requester_success() {
            // given
            Long userId = 1L;
            Long applicationId = 1L;

            ArtistApplication application = ArtistApplication.create(
                    userId,
                    "아이유",
                    "가수",
                    "https://example.com/iu.jpg",
                    ArtistType.SOLO
            );

            // 상세 조회 검증을 위해 엔티티 필드를 직접 주입한다.
            ReflectionTestUtils.setField(application, "id", applicationId);
            ReflectionTestUtils.setField(application, "createdAt",
                    LocalDateTime.of(2026, 4, 14, 10, 0, 0));
            ReflectionTestUtils.setField(application, "updatedAt",
                    LocalDateTime.of(2026, 4, 14, 11, 0, 0));

            User user = mock(User.class);
            when(user.getRole()).thenReturn(UserRole.USER);

            when(artistApplicationRepository.findById(applicationId))
                    .thenReturn(java.util.Optional.of(application));
            when(userRepository.findById(userId))
                    .thenReturn(java.util.Optional.of(user));

            // when
            ArtistApplicationDetailResponse response =
                    artistService.getArtistApplication(userId, applicationId);

            // then
            assertThat(response.applicationId()).isEqualTo(applicationId);
            assertThat(response.requesterUserId()).isEqualTo(userId);
            assertThat(response.requestedName()).isEqualTo("아이유");
            assertThat(response.artistType()).isEqualTo(ArtistType.SOLO.name());
            assertThat(response.bio()).isEqualTo("가수");
            assertThat(response.profileImageUrl()).isEqualTo("https://example.com/iu.jpg");
            assertThat(response.status()).isEqualTo("PENDING");
            assertThat(response.reviewedByAdminId()).isNull();
            assertThat(response.reviewedAt()).isNull();
            assertThat(response.rejectionReason()).isNull();
            assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 4, 14, 10, 0, 0));
            assertThat(response.updatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 14, 11, 0, 0));

            verify(artistApplicationRepository, times(1)).findById(applicationId);
            verify(userRepository, times(1)).findById(userId);
        }

        @Test
        @DisplayName("관리자는 아티스트 등록 요청 상세를 조회할 수 있다")
        void getArtistApplication_admin_success() {
            // given
            Long userId = 99L;
            Long applicationId = 1L;

            ArtistApplication application = ArtistApplication.create(
                    1L,
                    "아이유",
                    "가수",
                    "https://example.com/iu.jpg",
                    ArtistType.SOLO
            );

            // 상세 조회 검증을 위해 엔티티 필드를 직접 주입한다.
            ReflectionTestUtils.setField(application, "id", applicationId);
            ReflectionTestUtils.setField(application, "createdAt",
                    LocalDateTime.of(2026, 4, 14, 10, 0, 0));
            ReflectionTestUtils.setField(application, "updatedAt",
                    LocalDateTime.of(2026, 4, 14, 11, 0, 0));

            User adminUser = mock(User.class);
            when(adminUser.getRole()).thenReturn(UserRole.ADMIN);

            when(artistApplicationRepository.findById(applicationId))
                    .thenReturn(java.util.Optional.of(application));
            when(userRepository.findById(userId))
                    .thenReturn(java.util.Optional.of(adminUser));

            // when
            ArtistApplicationDetailResponse response =
                    artistService.getArtistApplication(userId, applicationId);

            // then
            assertThat(response.applicationId()).isEqualTo(applicationId);
            assertThat(response.requesterUserId()).isEqualTo(1L);
            assertThat(response.requestedName()).isEqualTo("아이유");
            assertThat(response.artistType()).isEqualTo(ArtistType.SOLO.name());
            assertThat(response.status()).isEqualTo("PENDING");

            verify(artistApplicationRepository, times(1)).findById(applicationId);
            verify(userRepository, times(1)).findById(userId);
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
                    "가수",
                    "https://example.com/iu.jpg",
                    ArtistType.SOLO
            );

            User user = mock(User.class);
            when(user.getRole()).thenReturn(UserRole.USER);

            when(artistApplicationRepository.findById(applicationId))
                    .thenReturn(java.util.Optional.of(application));
            when(userRepository.findById(userId))
                    .thenReturn(java.util.Optional.of(user));

            // when & then
            assertThatThrownBy(() -> artistService.getArtistApplication(userId, applicationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_DETAIL_FORBIDDEN.getMessage());

            verify(artistApplicationRepository, times(1)).findById(applicationId);
            verify(userRepository, times(1)).findById(userId);
        }

        @Test
        @DisplayName("존재하지 않는 아티스트 등록 요청이면 예외가 발생한다")
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
    @DisplayName("아티스트 등록 요청 승인")
    class ApproveArtistApplication {

        @Test
        @DisplayName("아티스트 등록 요청 승인에 성공한다")
        void approveArtistApplication_success() {
            // given
            Long adminId = 1L;
            Long applicationId = 10L;

            ArtistApplication application = ArtistApplication.create(
                    2L,
                    "아이유",
                    "가수",
                    "https://example.com/profile.jpg",
                    ArtistType.SOLO
            );
            ReflectionTestUtils.setField(application, "id", applicationId);

            Artist savedArtist = Artist.create(
                    application.getRequesterUserId(),
                    application.getRequestedName(),
                    application.getBio(),
                    application.getProfileImageUrl(),
                    application.getArtistType()
            );
            ReflectionTestUtils.setField(savedArtist, "id", 100L);

            when(artistApplicationRepository.findById(applicationId))
                    .thenReturn(java.util.Optional.of(application));
            when(artistRepository.save(any(Artist.class)))
                    .thenReturn(savedArtist);

            // when
            ArtistApplicationApproveResponse response =
                    artistService.approveArtistApplication(adminId, applicationId);

            // then
            assertThat(response.applicationId()).isEqualTo(applicationId);
            assertThat(response.artistId()).isEqualTo(100L);
            assertThat(response.status()).isEqualTo(ApplicationStatus.APPROVED.name());
            assertThat(response.reviewedByAdminId()).isEqualTo(adminId);
            assertThat(response.reviewedAt()).isNotNull();

            verify(artistApplicationRepository, times(1)).findById(applicationId);
            verify(artistRepository, times(1)).save(any(Artist.class));
        }

        @Test
        @DisplayName("이미 처리된 아티스트 등록 요청이면 승인에 실패한다")
        void approveArtistApplication_fail_whenAlreadyProcessed() {
            // given
            Long adminId = 1L;
            Long applicationId = 10L;

            ArtistApplication application = ArtistApplication.create(
                    2L,
                    "아이유",
                    "가수",
                    "https://example.com/profile.jpg",
                    ArtistType.SOLO
            );
            ReflectionTestUtils.setField(application, "id", applicationId);
            application.approve(adminId);

            when(artistApplicationRepository.findById(applicationId))
                    .thenReturn(java.util.Optional.of(application));

            // when & then
            assertThatThrownBy(() -> artistService.approveArtistApplication(adminId, applicationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_ALREADY_PROCESSED.getMessage());

            verify(artistApplicationRepository, times(1)).findById(applicationId);
            verify(artistRepository, never()).save(any(Artist.class));
        }
    }

    @Nested
    @DisplayName("아티스트 등록 요청 거절")
    class RejectArtistApplication {

        @Test
        @DisplayName("아티스트 등록 요청 거절에 성공한다")
        void rejectArtistApplication_success() {
            // given
            Long adminId = 1L;
            Long applicationId = 10L;
            ArtistApplicationRejectRequest request =
                    new ArtistApplicationRejectRequest("정보 부족");

            ArtistApplication application = ArtistApplication.create(
                    2L,
                    "아이유",
                    "가수",
                    "https://example.com/profile.jpg",
                    ArtistType.SOLO
            );
            ReflectionTestUtils.setField(application, "id", applicationId);

            when(artistApplicationRepository.findById(applicationId))
                    .thenReturn(java.util.Optional.of(application));

            // when
            ArtistApplicationRejectResponse response =
                    artistService.rejectArtistApplication(adminId, applicationId, request);

            // then
            assertThat(response.applicationId()).isEqualTo(applicationId);
            assertThat(response.status()).isEqualTo(ApplicationStatus.REJECTED.name());
            assertThat(response.reviewedByAdminId()).isEqualTo(adminId);
            assertThat(response.reviewedAt()).isNotNull();
            assertThat(response.rejectionReason()).isEqualTo("정보 부족");

            verify(artistApplicationRepository, times(1)).findById(applicationId);
            verify(artistRepository, never()).save(any(Artist.class));
        }

        @Test
        @DisplayName("이미 처리된 아티스트 등록 요청이면 거절에 실패한다")
        void rejectArtistApplication_fail_whenAlreadyProcessed() {
            // given
            Long adminId = 1L;
            Long applicationId = 10L;
            ArtistApplicationRejectRequest request =
                    new ArtistApplicationRejectRequest("정보 부족");

            ArtistApplication application = ArtistApplication.create(
                    2L,
                    "아이유",
                    "가수",
                    "https://example.com/profile.jpg",
                    ArtistType.SOLO
            );
            ReflectionTestUtils.setField(application, "id", applicationId);
            application.reject(adminId, "기존 거절 사유");

            when(artistApplicationRepository.findById(applicationId))
                    .thenReturn(java.util.Optional.of(application));

            // when & then
            assertThatThrownBy(() -> artistService.rejectArtistApplication(adminId, applicationId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_ALREADY_PROCESSED.getMessage());

            verify(artistApplicationRepository, times(1)).findById(applicationId);
            verify(artistRepository, never()).save(any(Artist.class));
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
            when(userRepository.findById(userId))
                    .thenReturn(java.util.Optional.of(user));

            Artist firstArtist = Artist.create(
                    userId,
                    "아이유",
                    "가수",
                    "https://example.com/iu.jpg",
                    ArtistType.SOLO
            );
            Artist secondArtist = Artist.create(
                    userId,
                    "아이유 밴드",
                    "프로젝트 아티스트",
                    "https://example.com/band.jpg",
                    ArtistType.COLLABORATION
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
            assertThat(response.get(0).artistType()).isEqualTo(ArtistType.SOLO.name());
            assertThat(response.get(0).bio()).isEqualTo("가수");
            assertThat(response.get(0).profileImageUrl()).isEqualTo("https://example.com/iu.jpg");
            assertThat(response.get(0).createdAt()).isEqualTo(LocalDateTime.of(2026, 4, 15, 10, 0, 0));
            assertThat(response.get(0).updatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 15, 11, 0, 0));

            assertThat(response.get(1).artistId()).isEqualTo(1L);
            assertThat(response.get(1).name()).isEqualTo("아이유 밴드");
            assertThat(response.get(1).artistType()).isEqualTo(ArtistType.COLLABORATION.name());
            assertThat(response.get(1).bio()).isEqualTo("프로젝트 아티스트");
            assertThat(response.get(1).profileImageUrl()).isEqualTo("https://example.com/band.jpg");
            assertThat(response.get(1).createdAt()).isEqualTo(LocalDateTime.of(2026, 4, 14, 10, 0, 0));
            assertThat(response.get(1).updatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 14, 11, 0, 0));

            verify(userRepository, times(1)).findById(userId);
            verify(artistRepository, times(1)).findMyArtists(userId);
        }

        @Test
        @DisplayName("소유한 아티스트가 없으면 빈 목록을 반환한다")
        void getMyArtists_empty() {
            // given
            Long userId = 1L;

            User user = mock(User.class);
            when(userRepository.findById(userId))
                    .thenReturn(java.util.Optional.of(user));

            when(artistRepository.findMyArtists(userId))
                    .thenReturn(List.of());

            // when
            List<MyArtistResponse> response = artistService.getMyArtists(userId);

            // then
            assertThat(response).isEmpty();

            verify(userRepository, times(1)).findById(userId);
            verify(artistRepository, times(1)).findMyArtists(userId);
        }
    }
}