package com.fivefy.domain.album.service;

import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.album.dto.request.AlbumApplicationCreateRequest;
import com.fivefy.domain.album.dto.response.*;
import com.fivefy.domain.album.entity.Album;
import com.fivefy.domain.album.entity.AlbumApplication;
import com.fivefy.domain.album.enums.AlbumApplicationErrorCode;
import com.fivefy.domain.album.enums.AlbumErrorCode;
import com.fivefy.domain.album.enums.AlbumStatus;
import com.fivefy.domain.album.repository.AlbumApplicationRepository;
import com.fivefy.domain.album.repository.AlbumRepository;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.artist.enums.ArtistErrorCode;
import com.fivefy.domain.artist.enums.ArtistStatus;
import com.fivefy.domain.artist.enums.ArtistType;
import com.fivefy.domain.artist.repository.ArtistRepository;
import com.fivefy.domain.track.entity.Track;
import com.fivefy.domain.track.repository.TrackRepository;
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
 * AlbumService의 비즈니스 로직검증하는 단위 테스트
 *
 * 앨범 등록 신청 생성 기능 검증
 * 내 앨범 등록 신청 목록 조회 기능 검증
 * 내 앨범 등록 신청 목록 빈 결과 조회 기능 검증
 * 앨범 등록 신청 상세 조회 기능 검증
 * 앨범 등록 신청 목록 조회 기능 검증
 * 앨범 등록 신청 승인 기능 검증
 * 앨범 등록 신청 거절 기능 검증
 * 앨범 상세 조회 기능 검증
 * 아티스트별 앨범 목록 조회 기능 검증
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

    @Mock
    private TrackRepository trackRepository;

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
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

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
            when(albumApplicationRepository.existsApprovedApplication(userId, artistId, request.title()))
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

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

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
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
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
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

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
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

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
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

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
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

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
        void createAlbumApplication_fail_whenPendingApplicationAlreadyExists() {
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
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

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

        @Test
        @DisplayName("동일한 처리 완료 신청이 이미 있으면 앨범 등록 신청 생성 실패")
        void createAlbumApplication_fail_whenApprovedApplicationAlreadyExists() {
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
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

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
            when(albumApplicationRepository.existsApprovedApplication(userId, artistId, request.title()))
                    .thenReturn(true);

            assertThatThrownBy(() -> albumService.createAlbumApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_ALREADY_PROCESSED.getMessage());
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
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

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
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
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

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

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
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

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
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(admin));

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
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

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
    @DisplayName("앨범 등록 신청 목록 조회")
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

            when(albumApplicationRepository.findByIdForUpdate(applicationId)).thenReturn(Optional.of(application));
            when(albumRepository.save(any(Album.class))).thenReturn(savedAlbum);

            AlbumApplicationApproveResponse response =
                    albumService.approveAlbumApplication(adminId, applicationId);

            assertThat(response.applicationId()).isEqualTo(applicationId);
            assertThat(response.albumId()).isEqualTo(1000L);
            assertThat(response.status()).isEqualTo(ApplicationStatus.APPROVED);
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

            when(albumApplicationRepository.findByIdForUpdate(applicationId)).thenReturn(Optional.of(application));
            when(albumRepository.save(any(Album.class))).thenReturn(savedAlbum);

            AlbumApplicationApproveResponse response =
                    albumService.approveAlbumApplication(adminId, applicationId);

            assertThat(response.applicationId()).isEqualTo(applicationId);
            assertThat(response.albumId()).isEqualTo(1000L);
            assertThat(response.status()).isEqualTo(ApplicationStatus.APPROVED);
            assertThat(response.reviewedByAdminId()).isEqualTo(adminId);
            assertThat(response.reviewedAt()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 신청이면 승인 실패")
        void approveAlbumApplication_fail_whenNotFound() {
            Long adminId = 1L;
            Long applicationId = 10L;

            when(albumApplicationRepository.findByIdForUpdate(applicationId)).thenReturn(Optional.empty());

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

            when(albumApplicationRepository.findByIdForUpdate(applicationId)).thenReturn(Optional.of(application));

            assertThatThrownBy(() -> albumService.approveAlbumApplication(adminId, applicationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_ALREADY_PROCESSED.getMessage());
        }
    }

    @Nested
    @DisplayName("앨범 등록 신청 거절")
    class RejectAlbumApplication {

        @Test
        @DisplayName("거절 성공")
        void rejectAlbumApplication_success() {
            Long adminId = 1L;
            Long applicationId = 10L;
            String rejectionReason = "앨범 정보가 부족합니다";

            AlbumApplication application = AlbumApplication.create(
                    2L,
                    100L,
                    "Palette",
                    "정규 앨범",
                    "https://example.com/album.jpg",
                    0
            );
            ReflectionTestUtils.setField(application, "id", applicationId);

            when(albumApplicationRepository.findByIdForUpdate(applicationId)).thenReturn(Optional.of(application));

            AlbumApplicationRejectResponse response =
                    albumService.rejectAlbumApplication(adminId, applicationId, rejectionReason);

            assertThat(response.applicationId()).isEqualTo(applicationId);
            assertThat(response.status()).isEqualTo(ApplicationStatus.REJECTED);
            assertThat(response.reviewedByAdminId()).isEqualTo(adminId);
            assertThat(response.reviewedAt()).isNotNull();
            assertThat(response.rejectionReason()).isEqualTo(rejectionReason);
        }

        @Test
        @DisplayName("존재하지 않는 신청이면 거절 실패")
        void rejectAlbumApplication_fail_whenNotFound() {
            Long adminId = 1L;
            Long applicationId = 10L;

            when(albumApplicationRepository.findByIdForUpdate(applicationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> albumService.rejectAlbumApplication(adminId, applicationId, "사유"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("이미 처리된 신청이면 거절 실패")
        void rejectAlbumApplication_fail_whenAlreadyProcessed() {
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

            when(albumApplicationRepository.findByIdForUpdate(applicationId)).thenReturn(Optional.of(application));

            assertThatThrownBy(() -> albumService.rejectAlbumApplication(adminId, applicationId, "사유"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_ALREADY_PROCESSED.getMessage());
        }
    }

    @Nested
    @DisplayName("앨범 상세 조회")
    class GetAlbum {

        @Test
        @DisplayName("조회 성공")
        void getAlbum_success() {
            Long albumId = 1L;

            Album album = Album.create(
                    10L,
                    "Palette",
                    "정규 앨범",
                    "url",
                    null
            );
            album.publish();
            ReflectionTestUtils.setField(album, "id", albumId);

            Artist artist = mock(Artist.class);
            when(artist.getName()).thenReturn("아이유");
            when(artist.getStatus()).thenReturn(ArtistStatus.ACTIVE);

            when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));
            when(artistRepository.findById(10L)).thenReturn(Optional.of(artist));

            Track track1 = mock(Track.class);
            when(track1.getId()).thenReturn(100L);
            when(track1.getTrackNumber()).thenReturn(1L);
            when(track1.getTitle()).thenReturn("이름에게");
            when(track1.getDurationSec()).thenReturn(245L);

            Track track2 = mock(Track.class);
            when(track2.getId()).thenReturn(101L);
            when(track2.getTrackNumber()).thenReturn(2L);
            when(track2.getTitle()).thenReturn("밤편지");
            when(track2.getDurationSec()).thenReturn(230L);

            when(trackRepository.searchAlbumTracks(albumId))
                    .thenReturn(List.of(track1, track2));

            AlbumDetailResponse response = albumService.getAlbum(albumId);

            assertThat(response.albumId()).isEqualTo(albumId);
            assertThat(response.artistName()).isEqualTo("아이유");
            assertThat(response.tracks()).hasSize(2);
            assertThat(response.tracks().get(0).trackId()).isEqualTo(100L);
            assertThat(response.tracks().get(0).trackNumber()).isEqualTo(1L);
            assertThat(response.tracks().get(0).title()).isEqualTo("이름에게");
            assertThat(response.tracks().get(0).durationSec()).isEqualTo(245L);
            assertThat(response.tracks().get(1).trackId()).isEqualTo(101L);
            assertThat(response.tracks().get(1).trackNumber()).isEqualTo(2L);
            assertThat(response.tracks().get(1).title()).isEqualTo("밤편지");
            assertThat(response.tracks().get(1).durationSec()).isEqualTo(230L);
        }

        @Test
        @DisplayName("앨범 없으면 실패")
        void getAlbum_fail_notFound() {
            when(albumRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> albumService.getAlbum(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AlbumErrorCode.ERR_ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("삭제된 앨범이면 상세 조회 실패")
        void getAlbum_fail_whenDeleted() {
            Long albumId = 1L;

            Album album = Album.create(
                    10L,
                    "Palette",
                    "정규 앨범",
                    "https://example.com/album.jpg",
                    null
            );
            ReflectionTestUtils.setField(album, "id", albumId);
            ReflectionTestUtils.setField(album, "deletedAt", LocalDateTime.of(2026, 4, 17, 12, 0, 0));

            when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));

            assertThatThrownBy(() -> albumService.getAlbum(albumId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AlbumErrorCode.ERR_ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("공개되지 않은 앨범이면 상세 조회 실패")
        void getAlbum_fail_whenNotPublished() {
            Long albumId = 1L;

            Album album = Album.create(
                    10L,
                    "Palette",
                    "정규 앨범",
                    "https://example.com/album.jpg",
                    null
            );
            ReflectionTestUtils.setField(album, "id", albumId);
            ReflectionTestUtils.setField(album, "status", AlbumStatus.UNPUBLISHED);

            when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));

            assertThatThrownBy(() -> albumService.getAlbum(albumId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AlbumErrorCode.ERR_ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("삭제된 아티스트의 앨범이면 상세 조회 실패")
        void getAlbum_fail_whenArtistDeleted() {
            Long albumId = 1L;
            Long artistId = 10L;

            Album album = Album.create(
                    artistId,
                    "Palette",
                    "정규 앨범",
                    "https://example.com/album.jpg",
                    null
            );
            album.publish();
            ReflectionTestUtils.setField(album, "id", albumId);

            Artist artist = Artist.create(
                    1L,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);
            ReflectionTestUtils.setField(artist, "deletedAt", LocalDateTime.of(2026, 4, 17, 12, 0, 0));

            when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));
            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));

            assertThatThrownBy(() -> albumService.getAlbum(albumId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AlbumErrorCode.ERR_ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("수록곡이 없어도 앨범 상세 조회 성공")
        void getAlbum_success_whenTracksEmpty() {
            Long albumId = 1L;

            Album album = Album.create(
                    10L,
                    "Palette",
                    "정규 앨범",
                    "url",
                    null
            );
            album.publish();
            ReflectionTestUtils.setField(album, "id", albumId);

            Artist artist = mock(Artist.class);
            when(artist.getName()).thenReturn("아이유");
            when(artist.isDeleted()).thenReturn(false);
            when(artist.getStatus()).thenReturn(ArtistStatus.ACTIVE);

            when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));
            when(artistRepository.findById(10L)).thenReturn(Optional.of(artist));
            when(trackRepository.searchAlbumTracks(albumId)).thenReturn(List.of());

            AlbumDetailResponse response = albumService.getAlbum(albumId);

            assertThat(response.albumId()).isEqualTo(albumId);
            assertThat(response.artistName()).isEqualTo("아이유");
            assertThat(response.tracks()).isEmpty();
        }
    }

    @Nested
    @DisplayName("아티스트별 앨범 목록 조회")
    class GetArtistAlbums {

        @Test
        @DisplayName("실제 공개된 시간 기준 내림차순 목록 조회 성공")
        void getArtistAlbums_success() {
            Long artistId = 10L;

            Artist artist = Artist.create(
                    1L,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);

            Album album1 = Album.create(
                    artistId,
                    "Palette",
                    "정규 앨범",
                    "https://example.com/album1.jpg",
                    null
            );
            album1.publish();
            ReflectionTestUtils.setField(album1, "id", 100L);
            ReflectionTestUtils.setField(album1, "trackCount", 10L);
            ReflectionTestUtils.setField(album1, "publishedAt", LocalDateTime.of(2026, 5, 1, 18, 0, 0));

            Album album2 = Album.create(
                    artistId,
                    "Love poem",
                    "미니 앨범",
                    "https://example.com/album2.jpg",
                    null
            );
            album2.publish();
            ReflectionTestUtils.setField(album2, "id", 101L);
            ReflectionTestUtils.setField(album2, "trackCount", 6L);
            ReflectionTestUtils.setField(album2, "publishedAt", LocalDateTime.of(2026, 4, 1, 18, 0, 0));

            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));
            when(albumRepository.searchArtistAlbums(artistId))
                    .thenReturn(List.of(album1, album2));

            List<ArtistAlbumListResponse> response =
                    albumService.getArtistAlbums(artistId);

            assertThat(response).hasSize(2);
            assertThat(response.get(0).albumId()).isEqualTo(100L);
            assertThat(response.get(0).title()).isEqualTo("Palette");
            assertThat(response.get(0).trackCount()).isEqualTo(10L);
            assertThat(response.get(1).albumId()).isEqualTo(101L);
        }

        @Test
        @DisplayName("공개된 앨범이 없으면 빈 목록 조회 성공")
        void getArtistAlbums_success_whenEmpty() {
            Long artistId = 10L;

            Artist artist = Artist.create(
                    1L,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);

            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));
            when(albumRepository.searchArtistAlbums(artistId))
                    .thenReturn(List.of());

            List<ArtistAlbumListResponse> response =
                    albumService.getArtistAlbums(artistId);

            assertThat(response).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 아티스트면 목록 조회 실패")
        void getArtistAlbums_fail_whenArtistNotFound() {
            Long artistId = 10L;

            when(artistRepository.findById(artistId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> albumService.getArtistAlbums(artistId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_ARTIST_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("삭제된 아티스트면 목록 조회 실패")
        void getArtistAlbums_fail_whenArtistDeleted() {
            Long artistId = 10L;

            Artist artist = Artist.create(
                    1L,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);
            ReflectionTestUtils.setField(artist, "deletedAt", LocalDateTime.of(2026, 4, 17, 12, 0, 0));

            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));

            assertThatThrownBy(() -> albumService.getArtistAlbums(artistId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_ARTIST_NOT_FOUND.getMessage());
        }
    }
}