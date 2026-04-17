package com.fivefy.domain.album.service;

import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.album.dto.request.AlbumApplicationCreateRequest;
import com.fivefy.domain.album.dto.response.AlbumApplicationApproveResponse;
import com.fivefy.domain.album.dto.response.AlbumApplicationDetailResponse;
import com.fivefy.domain.album.dto.response.AlbumApplicationListResponse;
import com.fivefy.domain.album.dto.response.AlbumApplicationResponse;
import com.fivefy.domain.album.entity.Album;
import com.fivefy.domain.album.entity.AlbumApplication;
import com.fivefy.domain.album.enums.AlbumApplicationErrorCode;
import com.fivefy.domain.album.repository.AlbumApplicationRepository;
import com.fivefy.domain.album.repository.AlbumRepository;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.artist.enums.ArtistErrorCode;
import com.fivefy.domain.artist.enums.ArtistType;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AlbumService의 비즈니스 로직을 검증하는 단위 테스트
 *
 * 앨범 등록 신청 생성 기능을 검증한다.
 * 내 앨범 등록 신청 목록 조회 기능을 검증한다.
 * 내 앨범 등록 신청 목록 빈 결과 조회 기능을 검증한다.
 * 앨범 등록 신청 상세 조회 기능을 검증한다.
 * 관리자 앨범 등록 신청 목록 조회 기능을 검증한다.
 * 앨범 등록 신청 승인 기능을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AlbumServiceTest {

    @Mock
    private AlbumApplicationRepository albumApplicationRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AlbumRepository albumRepository;

    @InjectMocks
    private AlbumService albumService;

    @Nested
    @DisplayName("앨범 등록 신청 생성")
    class CreateAlbumApplication {

        @Test
        @DisplayName("앨범 등록 신청 생성 성공")
        void createAlbumApplication_success() {
            Long userId = 1L;
            Long artistId = 10L;

            AlbumApplicationCreateRequest request = new AlbumApplicationCreateRequest(
                    artistId,
                    "Love poem",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    3
            );

            User user = mock(User.class);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            Artist artist = Artist.create(
                    userId,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);

            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));
            when(albumApplicationRepository.existsPendingApplication(userId, artistId, request.title()))
                    .thenReturn(false);

            AlbumApplication savedApplication = AlbumApplication.create(
                    userId,
                    artistId,
                    request.title(),
                    request.description(),
                    request.coverImageUrl(),
                    request.publishDelayDays()
            );
            ReflectionTestUtils.setField(savedApplication, "id", 1L);
            ReflectionTestUtils.setField(savedApplication, "createdAt", LocalDateTime.of(2026, 4, 16, 16, 0, 0));

            when(albumApplicationRepository.save(any(AlbumApplication.class)))
                    .thenReturn(savedApplication);

            AlbumApplicationResponse response =
                    albumService.createAlbumApplication(userId, request);

            assertThat(response.applicationId()).isEqualTo(1L);
            assertThat(response.artistId()).isEqualTo(artistId);
            assertThat(response.title()).isEqualTo("Love poem");
            assertThat(response.status()).isEqualTo(ApplicationStatus.PENDING);
            assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 4, 16, 16, 0, 0));
        }

        @Test
        @DisplayName("존재하지 않는 유저면 앨범 등록 신청 생성 실패")
        void createAlbumApplication_fail_whenUserNotFound() {
            Long userId = 1L;

            AlbumApplicationCreateRequest request = new AlbumApplicationCreateRequest(
                    10L,
                    "Love poem",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    0
            );

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> albumService.createAlbumApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(UserErrorCode.ERR_USER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("존재하지 않는 아티스트면 앨범 등록 신청 생성 실패")
        void createAlbumApplication_fail_whenArtistNotFound() {
            Long userId = 1L;
            Long artistId = 10L;

            AlbumApplicationCreateRequest request = new AlbumApplicationCreateRequest(
                    artistId,
                    "Love poem",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    0
            );

            User user = mock(User.class);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(artistRepository.findById(artistId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> albumService.createAlbumApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_ARTIST_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("삭제된 아티스트면 앨범 등록 신청 생성 실패")
        void createAlbumApplication_fail_whenArtistDeleted() {
            Long userId = 1L;
            Long artistId = 10L;

            AlbumApplicationCreateRequest request = new AlbumApplicationCreateRequest(
                    artistId,
                    "Love poem",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    0
            );

            User user = mock(User.class);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            Artist artist = Artist.create(
                    userId,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);
            ReflectionTestUtils.setField(artist, "deletedAt", LocalDateTime.of(2026, 4, 16, 16, 0, 0));

            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));

            assertThatThrownBy(() -> albumService.createAlbumApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_ARTIST_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("아티스트 소유자가 아니면 앨범 등록 신청 생성 실패")
        void createAlbumApplication_fail_whenForbiddenArtistAccess() {
            Long userId = 1L;
            Long artistId = 10L;

            AlbumApplicationCreateRequest request = new AlbumApplicationCreateRequest(
                    artistId,
                    "Love poem",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    0
            );

            User user = mock(User.class);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            Artist artist = Artist.create(
                    2L,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);

            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));

            assertThatThrownBy(() -> albumService.createAlbumApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_FORBIDDEN_ARTIST_ACCESS.getMessage());
        }

        @Test
        @DisplayName("비활성화된 아티스트면 앨범 등록 신청 생성 실패")
        void createAlbumApplication_fail_whenArtistInactive() {
            Long userId = 1L;
            Long artistId = 10L;

            AlbumApplicationCreateRequest request = new AlbumApplicationCreateRequest(
                    artistId,
                    "Love poem",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    0
            );

            User user = mock(User.class);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            Artist artist = Artist.create(
                    userId,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);
            artist.deactivate();

            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));

            assertThatThrownBy(() -> albumService.createAlbumApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AlbumApplicationErrorCode.ERR_INACTIVE_ARTIST_CANNOT_REQUEST_ALBUM_APPLICATION.getMessage());
        }

        @Test
        @DisplayName("공개 예약 옵션이 범위를 벗어나면 앨범 등록 신청 생성 실패")
        void createAlbumApplication_fail_whenInvalidPublishDelayDays() {
            Long userId = 1L;
            Long artistId = 10L;

            AlbumApplicationCreateRequest request = new AlbumApplicationCreateRequest(
                    artistId,
                    "Love poem",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    8
            );

            User user = mock(User.class);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            Artist artist = Artist.create(
                    userId,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);

            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));

            assertThatThrownBy(() -> albumService.createAlbumApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AlbumApplicationErrorCode.ERR_INVALID_PUBLISH_DELAY_DAYS.getMessage());
        }

        @Test
        @DisplayName("동일한 진행 중 신청이 이미 있으면 앨범 등록 신청 생성 실패")
        void createAlbumApplication_fail_whenAlreadyExists() {
            Long userId = 1L;
            Long artistId = 10L;

            AlbumApplicationCreateRequest request = new AlbumApplicationCreateRequest(
                    artistId,
                    "Love poem",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    0
            );

            User user = mock(User.class);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            Artist artist = Artist.create(
                    userId,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);

            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));
            when(albumApplicationRepository.existsPendingApplication(userId, artistId, request.title()))
                    .thenReturn(true);

            assertThatThrownBy(() -> albumService.createAlbumApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_ALREADY_EXISTS.getMessage());
        }
    }

    @Nested
    @DisplayName("내 앨범 등록 신청 목록 조회")
    class GetMyAlbumApplications {

        @Test
        @DisplayName("최신순으로 목록 조회 성공")
        void getMyAlbumApplications_success() {
            Long userId = 1L;
            Long artistId = 10L;

            User user = mock(User.class);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            AlbumApplication application1 = AlbumApplication.create(
                    userId,
                    artistId,
                    "두 번째 신청",
                    "설명2",
                    "https://example.com/cover2.jpg",
                    2
            );
            ReflectionTestUtils.setField(application1, "id", 2L);
            ReflectionTestUtils.setField(application1, "createdAt", LocalDateTime.of(2026, 4, 16, 16, 0, 0));

            AlbumApplication application2 = AlbumApplication.create(
                    userId,
                    artistId,
                    "첫 번째 신청",
                    "설명1",
                    "https://example.com/cover1.jpg",
                    0
            );
            ReflectionTestUtils.setField(application2, "id", 1L);
            ReflectionTestUtils.setField(application2, "createdAt", LocalDateTime.of(2026, 4, 15, 16, 0, 0));

            when(albumApplicationRepository.searchMyAlbumApplications(userId))
                    .thenReturn(List.of(application1, application2));

            List<AlbumApplicationResponse> response =
                    albumService.getMyAlbumApplications(userId);

            assertThat(response).hasSize(2);
            assertThat(response.get(0).applicationId()).isEqualTo(2L);
            assertThat(response.get(0).title()).isEqualTo("두 번째 신청");
            assertThat(response.get(1).applicationId()).isEqualTo(1L);
            assertThat(response.get(1).title()).isEqualTo("첫 번째 신청");
        }

        @Test
        @DisplayName("내 앨범 등록 신청이 없으면 빈 목록 조회 성공")
        void getMyAlbumApplications_success_whenEmpty() {
            Long userId = 1L;

            User user = mock(User.class);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(albumApplicationRepository.searchMyAlbumApplications(userId))
                    .thenReturn(List.of());

            List<AlbumApplicationResponse> response =
                    albumService.getMyAlbumApplications(userId);

            assertThat(response).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 유저면 목록 조회 실패")
        void getMyAlbumApplications_fail_whenUserNotFound() {
            Long userId = 1L;

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> albumService.getMyAlbumApplications(userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(UserErrorCode.ERR_USER_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("앨범 등록 신청 상세 조회")
    class GetAlbumApplication {

        @Test
        @DisplayName("신청자 본인이면 상세 조회 성공")
        void getAlbumApplication_success_whenRequester() {
            Long userId = 1L;
            Long applicationId = 100L;
            Long artistId = 10L;

            User user = mock(User.class);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            AlbumApplication application = AlbumApplication.create(
                    userId,
                    artistId,
                    "Palette",
                    "정규 앨범",
                    "https://example.com/album.jpg",
                    0
            );
            ReflectionTestUtils.setField(application, "id", applicationId);
            ReflectionTestUtils.setField(application, "createdAt", LocalDateTime.of(2026, 4, 14, 15, 0, 0));
            ReflectionTestUtils.setField(application, "updatedAt", LocalDateTime.of(2026, 4, 14, 15, 0, 0));

            when(albumApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

            AlbumApplicationDetailResponse response =
                    albumService.getAlbumApplication(userId, applicationId);

            assertThat(response.applicationId()).isEqualTo(applicationId);
            assertThat(response.requesterUserId()).isEqualTo(userId);
            assertThat(response.artistId()).isEqualTo(artistId);
            assertThat(response.title()).isEqualTo("Palette");
            assertThat(response.publishDelayDays()).isEqualTo(0);
            assertThat(response.status()).isEqualTo(ApplicationStatus.PENDING);
        }

        @Test
        @DisplayName("관리자면 상세 조회 성공")
        void getAlbumApplication_success_whenAdmin() {
            Long userId = 99L;
            Long applicationId = 100L;
            Long requesterUserId = 1L;
            Long artistId = 10L;

            User admin = mock(User.class);
            when(admin.getRole()).thenReturn(UserRole.ADMIN);
            when(userRepository.findById(userId)).thenReturn(Optional.of(admin));

            AlbumApplication application = AlbumApplication.create(
                    requesterUserId,
                    artistId,
                    "Palette",
                    "정규 앨범",
                    "https://example.com/album.jpg",
                    0
            );
            ReflectionTestUtils.setField(application, "id", applicationId);
            ReflectionTestUtils.setField(application, "createdAt", LocalDateTime.of(2026, 4, 14, 15, 0, 0));
            ReflectionTestUtils.setField(application, "updatedAt", LocalDateTime.of(2026, 4, 14, 15, 0, 0));

            when(albumApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

            AlbumApplicationDetailResponse response =
                    albumService.getAlbumApplication(userId, applicationId);

            assertThat(response.applicationId()).isEqualTo(applicationId);
            assertThat(response.requesterUserId()).isEqualTo(requesterUserId);
        }

        @Test
        @DisplayName("본인도 관리자도 아니면 상세 조회 실패")
        void getAlbumApplication_fail_whenForbidden() {
            Long userId = 2L;
            Long applicationId = 100L;
            Long requesterUserId = 1L;
            Long artistId = 10L;

            User user = mock(User.class);
            when(user.getRole()).thenReturn(UserRole.USER);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            AlbumApplication application = AlbumApplication.create(
                    requesterUserId,
                    artistId,
                    "Palette",
                    "정규 앨범",
                    "https://example.com/album.jpg",
                    0
            );
            ReflectionTestUtils.setField(application, "id", applicationId);

            when(albumApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

            assertThatThrownBy(() -> albumService.getAlbumApplication(userId, applicationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_DETAIL_FORBIDDEN.getMessage());
        }

        @Test
        @DisplayName("존재하지 않는 신청이면 상세 조회 실패")
        void getAlbumApplication_fail_whenNotFound() {
            Long userId = 1L;
            Long applicationId = 100L;

            when(albumApplicationRepository.findById(applicationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> albumService.getAlbumApplication(userId, applicationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("관리자 앨범 등록 신청 목록 조회")
    class GetAlbumApplications {

        @Test
        @DisplayName("상태 조건 없이 오래된순 목록 조회 성공")
        void getAlbumApplications_success_withoutStatus() {
            Pageable pageable = PageRequest.of(0, 20);

            AlbumApplication application1 = AlbumApplication.create(
                    1L,
                    10L,
                    "첫 번째 신청",
                    "설명1",
                    "https://example.com/cover1.jpg",
                    0
            );
            ReflectionTestUtils.setField(application1, "id", 1L);
            ReflectionTestUtils.setField(application1, "createdAt", LocalDateTime.of(2026, 4, 14, 15, 0, 0));

            AlbumApplication application2 = AlbumApplication.create(
                    2L,
                    20L,
                    "두 번째 신청",
                    "설명2",
                    "https://example.com/cover2.jpg",
                    3
            );
            ReflectionTestUtils.setField(application2, "id", 2L);
            ReflectionTestUtils.setField(application2, "createdAt", LocalDateTime.of(2026, 4, 15, 15, 0, 0));

            when(albumApplicationRepository.searchAlbumApplications(null, pageable))
                    .thenReturn(new PageImpl<>(List.of(application1, application2), pageable, 2));

            PageResponse<AlbumApplicationListResponse> response =
                    albumService.getAlbumApplications(null, pageable);

            assertThat(response.content()).hasSize(2);
            assertThat(response.content().get(0).applicationId()).isEqualTo(1L);
            assertThat(response.content().get(0).requesterUserId()).isEqualTo(1L);
            assertThat(response.content().get(0).title()).isEqualTo("첫 번째 신청");
            assertThat(response.content().get(1).applicationId()).isEqualTo(2L);
            assertThat(response.page()).isEqualTo(0);
            assertThat(response.size()).isEqualTo(20);
            assertThat(response.totalElements()).isEqualTo(2);
            assertThat(response.totalPages()).isEqualTo(1);
        }

        @Test
        @DisplayName("상태 조건으로 목록 조회 성공")
        void getAlbumApplications_success_withStatus() {
            Pageable pageable = PageRequest.of(0, 20);

            AlbumApplication application = AlbumApplication.create(
                    1L,
                    10L,
                    "승인 대기 신청",
                    "설명",
                    "https://example.com/cover.jpg",
                    0
            );
            ReflectionTestUtils.setField(application, "id", 1L);
            ReflectionTestUtils.setField(application, "createdAt", LocalDateTime.of(2026, 4, 14, 15, 0, 0));

            when(albumApplicationRepository.searchAlbumApplications(ApplicationStatus.PENDING, pageable))
                    .thenReturn(new PageImpl<>(List.of(application), pageable, 1));

            PageResponse<AlbumApplicationListResponse> response =
                    albumService.getAlbumApplications(ApplicationStatus.PENDING, pageable);

            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).status()).isEqualTo(ApplicationStatus.PENDING);
            assertThat(response.totalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("조회 결과가 없으면 빈 페이지 조회 성공")
        void getAlbumApplications_success_whenEmpty() {
            Pageable pageable = PageRequest.of(0, 20);

            when(albumApplicationRepository.searchAlbumApplications(ApplicationStatus.REJECTED, pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            PageResponse<AlbumApplicationListResponse> response =
                    albumService.getAlbumApplications(ApplicationStatus.REJECTED, pageable);

            assertThat(response.content()).isEmpty();
            assertThat(response.totalElements()).isZero();
            assertThat(response.totalPages()).isZero();
        }
    }

    @Nested
    @DisplayName("앨범 등록 신청 승인")
    class ApproveAlbumApplication {

        @Test
        @DisplayName("즉시 공개 신청이면 승인과 함께 앨범 생성 및 공개 성공")
        void approveAlbumApplication_success_whenImmediatePublish() {
            Long adminId = 1L;
            Long applicationId = 10L;

            AlbumApplication application = AlbumApplication.create(
                    2L,
                    100L,
                    "Palette",
                    "정규 앨범",
                    "https://example.com/album.jpg",
                    0
            );
            ReflectionTestUtils.setField(application, "id", applicationId);

            Album savedAlbum = Album.create(
                    application.getArtistId(),
                    application.getTitle(),
                    application.getDescription(),
                    application.getCoverImageUrl(),
                    null
            );
            ReflectionTestUtils.setField(savedAlbum, "id", 1000L);

            when(albumApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(albumRepository.save(any(Album.class))).thenReturn(savedAlbum);

            AlbumApplicationApproveResponse response =
                    albumService.approveAlbumApplication(adminId, applicationId);

            assertThat(response.applicationId()).isEqualTo(applicationId);
            assertThat(response.albumId()).isEqualTo(1000L);
            assertThat(response.status()).isEqualTo(ApplicationStatus.APPROVED.name());
            assertThat(response.reviewedByAdminId()).isEqualTo(adminId);
            assertThat(response.reviewedAt()).isNotNull();
        }

        @Test
        @DisplayName("예약 공개 신청이면 승인과 함께 앨범 생성 성공")
        void approveAlbumApplication_success_whenScheduledPublish() {
            Long adminId = 1L;
            Long applicationId = 10L;

            AlbumApplication application = AlbumApplication.create(
                    2L,
                    100L,
                    "Palette",
                    "정규 앨범",
                    "https://example.com/album.jpg",
                    3
            );
            ReflectionTestUtils.setField(application, "id", applicationId);

            Album savedAlbum = Album.create(
                    application.getArtistId(),
                    application.getTitle(),
                    application.getDescription(),
                    application.getCoverImageUrl(),
                    LocalDateTime.now().plusDays(3)
            );
            ReflectionTestUtils.setField(savedAlbum, "id", 1000L);

            when(albumApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(albumRepository.save(any(Album.class))).thenReturn(savedAlbum);

            AlbumApplicationApproveResponse response =
                    albumService.approveAlbumApplication(adminId, applicationId);

            assertThat(response.applicationId()).isEqualTo(applicationId);
            assertThat(response.albumId()).isEqualTo(1000L);
            assertThat(response.status()).isEqualTo(ApplicationStatus.APPROVED.name());
            assertThat(response.reviewedByAdminId()).isEqualTo(adminId);
            assertThat(response.reviewedAt()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 신청이면 승인 실패")
        void approveAlbumApplication_fail_whenNotFound() {
            Long adminId = 1L;
            Long applicationId = 10L;

            when(albumApplicationRepository.findById(applicationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> albumService.approveAlbumApplication(adminId, applicationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("이미 처리된 신청이면 승인 실패")
        void approveAlbumApplication_fail_whenAlreadyProcessed() {
            Long adminId = 1L;
            Long applicationId = 10L;

            AlbumApplication application = AlbumApplication.create(
                    2L,
                    100L,
                    "Palette",
                    "정규 앨범",
                    "https://example.com/album.jpg",
                    0
            );
            ReflectionTestUtils.setField(application, "id", applicationId);
            application.approve(adminId);

            when(albumApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

            assertThatThrownBy(() -> albumService.approveAlbumApplication(adminId, applicationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_ALREADY_PROCESSED.getMessage());
        }
    }
}