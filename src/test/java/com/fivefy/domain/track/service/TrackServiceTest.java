package com.fivefy.domain.track.service;

import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.dto.response.SliceResponse;
import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.album.entity.Album;
import com.fivefy.domain.album.enums.AlbumErrorCode;
import com.fivefy.domain.album.repository.AlbumRepository;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.artist.enums.ArtistErrorCode;
import com.fivefy.domain.artist.enums.ArtistType;
import com.fivefy.domain.artist.repository.ArtistRepository;
import com.fivefy.domain.track.dto.cache.TrackDetailCache;
import com.fivefy.domain.track.dto.request.FreeTrackApplicationCreateRequest;
import com.fivefy.domain.track.dto.request.OfficialTrackApplicationCreateRequest;
import com.fivefy.domain.track.dto.response.*;
import com.fivefy.domain.track.entity.Track;
import com.fivefy.domain.track.entity.TrackApplication;
import com.fivefy.domain.track.entity.TrackComment;
import com.fivefy.domain.track.enums.TrackApplicationErrorCode;
import com.fivefy.domain.track.enums.TrackErrorCode;
import com.fivefy.domain.track.enums.TrackStatus;
import com.fivefy.domain.track.enums.TrackType;
import com.fivefy.domain.track.repository.*;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TrackService의 비즈니스 로직을 검증하는 단위 테스트
 *
 * 자유 창작 트랙 등록 신청 생성 기능 검증
 * 정식 발매 트랙 등록 신청 생성 기능 검증
 * 내 트랙 등록 신청 목록 조회 기능 검증
 * 트랙 등록 신청 상세 조회 기능 검증
 * 트랙 등록 신청 목록 조회 기능 검증
 * 트랙 등록 신청 승인 기능 검증
 * 트랙 등록 신청 거절 기능 검증
 * 트랙 상세 조회 기능 검증
 * 공개 트랙 목록 조회 기능 검증
 * 아티스트별 자유 창작 트랙 목록 조회 기능 검증
 */
@ExtendWith(MockitoExtension.class)
class TrackServiceTest {

    @Mock
    private TrackApplicationRepository trackApplicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private AlbumRepository albumRepository;

    @Mock
    private TrackRepository trackRepository;

    @Mock
    private TrackCommentRepository trackCommentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TrackDetailCacheService trackDetailCacheService;

    @InjectMocks
    private TrackService trackService;

    @Nested
    @DisplayName("자유 창작 트랙 등록 신청 생성")
    class CreateFreeTrackApplication {

        @Test
        @DisplayName("자유 창작 트랙 등록 신청 성공")
        void createFreeTrackApplication_success() {
            Long userId = 1L;

            FreeTrackApplicationCreateRequest request =
                    new FreeTrackApplicationCreateRequest(
                            "밤편지 AI 버전",
                            "가사",
                            "BALLAD",
                            "https://example.com/audio.mp3",
                            210L
                    );

            User user = mock(User.class);
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

            when(trackApplicationRepository.existsPendingFreeCreationApplication(
                    userId,
                    request.title(),
                    request.audioUrl()
            )).thenReturn(false);

            TrackApplication savedApplication = TrackApplication.create(
                    userId,
                    TrackType.FREE_CREATION,
                    null,
                    null,
                    null,
                    request.title(),
                    request.lyrics(),
                    request.genre(),
                    request.audioUrl(),
                    request.durationSec(),
                    null,
                    null
            );
            ReflectionTestUtils.setField(savedApplication, "id", 1L);
            ReflectionTestUtils.setField(
                    savedApplication,
                    "createdAt",
                    LocalDateTime.of(2026, 4, 19, 20, 0, 0)
            );

            when(trackApplicationRepository.save(any(TrackApplication.class)))
                    .thenReturn(savedApplication);

            TrackApplicationResponse response =
                    trackService.createFreeTrackApplication(userId, request);

            assertThat(response.applicationId()).isEqualTo(1L);
            assertThat(response.trackType()).isEqualTo(TrackType.FREE_CREATION);
            assertThat(response.artistId()).isNull();
            assertThat(response.albumId()).isNull();
            assertThat(response.title()).isEqualTo("밤편지 AI 버전");
            assertThat(response.status()).isEqualTo(savedApplication.getStatus());
            assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 4, 19, 20, 0, 0));
        }

        @Test
        @DisplayName("존재하지 않는 유저면 자유 창작 트랙 등록 신청 생성 실패")
        void createFreeTrackApplication_fail_whenUserNotFound() {
            Long userId = 1L;

            FreeTrackApplicationCreateRequest request =
                    new FreeTrackApplicationCreateRequest(
                            "밤편지 AI 버전",
                            "가사",
                            "BALLAD",
                            "https://example.com/audio.mp3",
                            210L
                    );

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> trackService.createFreeTrackApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(UserErrorCode.ERR_USER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("동일한 진행 중 신청이 이미 있으면 자유 창작 트랙 등록 신청 생성 실패")
        void createFreeTrackApplication_fail_whenAlreadyExists() {
            Long userId = 1L;

            FreeTrackApplicationCreateRequest request =
                    new FreeTrackApplicationCreateRequest(
                            "밤편지 AI 버전",
                            "가사",
                            "BALLAD",
                            "https://example.com/audio.mp3",
                            210L
                    );

            User user = mock(User.class);
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

            when(trackApplicationRepository.existsPendingFreeCreationApplication(
                    userId,
                    request.title(),
                    request.audioUrl()
            )).thenReturn(true);

            assertThatThrownBy(() -> trackService.createFreeTrackApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackApplicationErrorCode.ERR_TRACK_APPLICATION_ALREADY_EXISTS.getMessage());
        }
    }

    @Nested
    @DisplayName("정식 발매 트랙 등록 신청 생성")
    class CreateOfficialTrackApplication {

        @Test
        @DisplayName("정식 발매 트랙 등록 신청 성공")
        void createOfficialTrackApplication_success() {
            Long userId = 1L;
            Long artistId = 10L;
            Long albumId = 100L;

            OfficialTrackApplicationCreateRequest request =
                    new OfficialTrackApplicationCreateRequest(
                            artistId,
                            albumId,
                            1L,
                            "밤편지",
                            "가사",
                            "BALLAD",
                            "https://example.com/audio.mp3",
                            230L,
                            "feat. 10cm",
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

            Album album = Album.create(
                    artistId,
                    "Love poem",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    null
            );
            ReflectionTestUtils.setField(album, "id", albumId);

            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));
            when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));
            when(trackApplicationRepository.existsPendingOfficialReleaseApplication(
                    userId,
                    artistId,
                    albumId,
                    request.trackNumber(),
                    request.title()
            )).thenReturn(false);

            TrackApplication savedApplication = TrackApplication.create(
                    userId,
                    TrackType.OFFICIAL_RELEASE,
                    artistId,
                    albumId,
                    request.trackNumber(),
                    request.title(),
                    request.lyrics(),
                    request.genre(),
                    request.audioUrl(),
                    request.durationSec(),
                    request.featuredArtistText(),
                    request.publishDelayDays()
            );
            ReflectionTestUtils.setField(savedApplication, "id", 2L);
            ReflectionTestUtils.setField(
                    savedApplication,
                    "createdAt",
                    LocalDateTime.of(2026, 4, 19, 21, 0, 0)
            );

            when(trackApplicationRepository.save(any(TrackApplication.class)))
                    .thenReturn(savedApplication);

            TrackApplicationResponse response =
                    trackService.createOfficialTrackApplication(userId, request);

            assertThat(response.applicationId()).isEqualTo(2L);
            assertThat(response.trackType()).isEqualTo(TrackType.OFFICIAL_RELEASE);
            assertThat(response.artistId()).isEqualTo(artistId);
            assertThat(response.albumId()).isEqualTo(albumId);
            assertThat(response.title()).isEqualTo("밤편지");
            assertThat(response.status()).isEqualTo(ApplicationStatus.PENDING);
            assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 4, 19, 21, 0, 0));
        }

        @Test
        @DisplayName("존재하지 않는 유저면 정식 발매 트랙 등록 신청 생성 실패")
        void createOfficialTrackApplication_fail_whenUserNotFound() {
            Long userId = 1L;

            OfficialTrackApplicationCreateRequest request =
                    new OfficialTrackApplicationCreateRequest(
                            10L,
                            100L,
                            1L,
                            "밤편지",
                            "가사",
                            "BALLAD",
                            "https://example.com/audio.mp3",
                            230L,
                            "feat. 10cm",
                            3
                    );

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> trackService.createOfficialTrackApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(UserErrorCode.ERR_USER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("존재하지 않는 아티스트면 정식 발매 트랙 등록 신청 생성 실패")
        void createOfficialTrackApplication_fail_whenArtistNotFound() {
            Long userId = 1L;
            Long artistId = 10L;

            OfficialTrackApplicationCreateRequest request =
                    new OfficialTrackApplicationCreateRequest(
                            artistId,
                            100L,
                            1L,
                            "밤편지",
                            "가사",
                            "BALLAD",
                            "https://example.com/audio.mp3",
                            230L,
                            "feat. 10cm",
                            3
                    );

            User user = mock(User.class);
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
            when(artistRepository.findById(artistId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> trackService.createOfficialTrackApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_ARTIST_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("삭제된 아티스트면 정식 발매 트랙 등록 신청 생성 실패")
        void createOfficialTrackApplication_fail_whenArtistDeleted() {
            Long userId = 1L;
            Long artistId = 10L;

            OfficialTrackApplicationCreateRequest request =
                    new OfficialTrackApplicationCreateRequest(
                            artistId,
                            100L,
                            1L,
                            "밤편지",
                            "가사",
                            "BALLAD",
                            "https://example.com/audio.mp3",
                            230L,
                            "feat. 10cm",
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
            ReflectionTestUtils.setField(artist, "deletedAt", LocalDateTime.of(2026, 4, 19, 20, 0, 0));

            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));

            assertThatThrownBy(() -> trackService.createOfficialTrackApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_ARTIST_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("아티스트 소유자가 아니면 정식 발매 트랙 등록 신청 생성 실패")
        void createOfficialTrackApplication_fail_whenForbiddenArtistAccess() {
            Long userId = 1L;
            Long artistId = 10L;

            OfficialTrackApplicationCreateRequest request =
                    new OfficialTrackApplicationCreateRequest(
                            artistId,
                            100L,
                            1L,
                            "밤편지",
                            "가사",
                            "BALLAD",
                            "https://example.com/audio.mp3",
                            230L,
                            "feat. 10cm",
                            3
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

            assertThatThrownBy(() -> trackService.createOfficialTrackApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_FORBIDDEN_ARTIST_ACCESS.getMessage());
        }

        @Test
        @DisplayName("비활성화된 아티스트면 정식 발매 트랙 등록 신청 생성 실패")
        void createOfficialTrackApplication_fail_whenArtistInactive() {
            Long userId = 1L;
            Long artistId = 10L;

            OfficialTrackApplicationCreateRequest request =
                    new OfficialTrackApplicationCreateRequest(
                            artistId,
                            100L,
                            1L,
                            "밤편지",
                            "가사",
                            "BALLAD",
                            "https://example.com/audio.mp3",
                            230L,
                            "feat. 10cm",
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
            artist.deactivate();

            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));

            assertThatThrownBy(() -> trackService.createOfficialTrackApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackApplicationErrorCode.ERR_INACTIVE_ARTIST_CANNOT_REQUEST_OFFICIAL_RELEASE.getMessage());
        }

        @Test
        @DisplayName("존재하지 않는 앨범이면 정식 발매 트랙 등록 신청 생성 실패")
        void createOfficialTrackApplication_fail_whenAlbumNotFound() {
            Long userId = 1L;
            Long artistId = 10L;
            Long albumId = 100L;

            OfficialTrackApplicationCreateRequest request =
                    new OfficialTrackApplicationCreateRequest(
                            artistId,
                            albumId,
                            1L,
                            "밤편지",
                            "가사",
                            "BALLAD",
                            "https://example.com/audio.mp3",
                            230L,
                            "feat. 10cm",
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
            when(albumRepository.findById(albumId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> trackService.createOfficialTrackApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AlbumErrorCode.ERR_ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("삭제된 앨범이면 정식 발매 트랙 등록 신청 생성 실패")
        void createOfficialTrackApplication_fail_whenAlbumDeleted() {
            Long userId = 1L;
            Long artistId = 10L;
            Long albumId = 100L;

            OfficialTrackApplicationCreateRequest request =
                    new OfficialTrackApplicationCreateRequest(
                            artistId,
                            albumId,
                            1L,
                            "밤편지",
                            "가사",
                            "BALLAD",
                            "https://example.com/audio.mp3",
                            230L,
                            "feat. 10cm",
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

            Album album = Album.create(
                    artistId,
                    "Love poem",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    null
            );
            ReflectionTestUtils.setField(album, "id", albumId);
            ReflectionTestUtils.setField(album, "deletedAt", LocalDateTime.of(2026, 4, 19, 20, 0, 0));

            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));
            when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));

            assertThatThrownBy(() -> trackService.createOfficialTrackApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AlbumErrorCode.ERR_ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("앨범과 아티스트 정보가 일치하지 않으면 정식 발매 트랙 등록 신청 생성 실패")
        void createOfficialTrackApplication_fail_whenAlbumArtistMismatch() {
            Long userId = 1L;
            Long artistId = 10L;
            Long albumId = 100L;

            OfficialTrackApplicationCreateRequest request =
                    new OfficialTrackApplicationCreateRequest(
                            artistId,
                            albumId,
                            1L,
                            "밤편지",
                            "가사",
                            "BALLAD",
                            "https://example.com/audio.mp3",
                            230L,
                            "feat. 10cm",
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

            Album album = Album.create(
                    999L,
                    "Love poem",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    null
            );
            ReflectionTestUtils.setField(album, "id", albumId);

            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));
            when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));

            assertThatThrownBy(() -> trackService.createOfficialTrackApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackApplicationErrorCode.ERR_ALBUM_ARTIST_MISMATCH.getMessage());
        }

        @Test
        @DisplayName("공개 예약 옵션이 범위를 벗어나면 정식 발매 트랙 등록 신청 생성 실패")
        void createOfficialTrackApplication_fail_whenInvalidPublishDelayDays() {
            Long userId = 1L;
            Long artistId = 10L;
            Long albumId = 100L;

            OfficialTrackApplicationCreateRequest request =
                    new OfficialTrackApplicationCreateRequest(
                            artistId,
                            albumId,
                            1L,
                            "밤편지",
                            "가사",
                            "BALLAD",
                            "https://example.com/audio.mp3",
                            230L,
                            "feat. 10cm",
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

            Album album = Album.create(
                    artistId,
                    "Love poem",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    null
            );
            ReflectionTestUtils.setField(album, "id", albumId);

            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));
            when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));

            assertThatThrownBy(() -> trackService.createOfficialTrackApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackApplicationErrorCode.ERR_INVALID_PUBLISH_DELAY_DAYS.getMessage());
        }

        @Test
        @DisplayName("동일한 진행 중 신청이 이미 있으면 정식 발매 트랙 등록 신청 생성 실패")
        void createOfficialTrackApplication_fail_whenAlreadyExists() {
            Long userId = 1L;
            Long artistId = 10L;
            Long albumId = 100L;

            OfficialTrackApplicationCreateRequest request =
                    new OfficialTrackApplicationCreateRequest(
                            artistId,
                            albumId,
                            1L,
                            "밤편지",
                            "가사",
                            "BALLAD",
                            "https://example.com/audio.mp3",
                            230L,
                            "feat. 10cm",
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

            Album album = Album.create(
                    artistId,
                    "Love poem",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    null
            );
            ReflectionTestUtils.setField(album, "id", albumId);

            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));
            when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));
            when(trackApplicationRepository.existsPendingOfficialReleaseApplication(
                    userId,
                    artistId,
                    albumId,
                    request.trackNumber(),
                    request.title()
            )).thenReturn(true);

            assertThatThrownBy(() -> trackService.createOfficialTrackApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackApplicationErrorCode.ERR_TRACK_APPLICATION_ALREADY_EXISTS.getMessage());
        }

        @Nested
        @DisplayName("내 트랙 등록 신청 목록 조회")
        class GetMyTrackApplications {

            @Test
            @DisplayName("내 트랙 등록 신청 목록 조회 성공")
            void getMyTrackApplications_success() {
                Long userId = 1L;

                User user = mock(User.class);
                when(userRepository.findByIdAndDeletedAtIsNull(userId))
                        .thenReturn(Optional.of(user));

                TrackApplication application1 = mock(TrackApplication.class);
                TrackApplication application2 = mock(TrackApplication.class);

                when(trackApplicationRepository.searchMyTrackApplications(userId))
                        .thenReturn(List.of(application1, application2));

                when(application1.getId()).thenReturn(1L);
                when(application2.getId()).thenReturn(2L);

                when(application1.getTrackType()).thenReturn(TrackType.FREE_CREATION);
                when(application2.getTrackType()).thenReturn(TrackType.OFFICIAL_RELEASE);

                when(application1.getStatus()).thenReturn(ApplicationStatus.PENDING);
                when(application2.getStatus()).thenReturn(ApplicationStatus.PENDING);

                when(application1.getCreatedAt()).thenReturn(LocalDateTime.now());
                when(application2.getCreatedAt()).thenReturn(LocalDateTime.now());

                List<TrackApplicationResponse> response =
                        trackService.getMyTrackApplications(userId);

                assertThat(response).hasSize(2);
                assertThat(response.get(0).applicationId()).isEqualTo(1L);
                assertThat(response.get(1).applicationId()).isEqualTo(2L);
            }

            @Test
            @DisplayName("존재하지 않는 유저면 목록 조회 실패")
            void getMyTrackApplications_fail_whenUserNotFound() {
                Long userId = 1L;

                when(userRepository.findByIdAndDeletedAtIsNull(userId))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() -> trackService.getMyTrackApplications(userId))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(UserErrorCode.ERR_USER_NOT_FOUND.getMessage());
            }

            @Test
            @DisplayName("등록 신청이 없으면 빈 리스트 반환")
            void getMyTrackApplications_empty() {
                Long userId = 1L;

                User user = mock(User.class);
                when(userRepository.findByIdAndDeletedAtIsNull(userId))
                        .thenReturn(Optional.of(user));

                when(trackApplicationRepository.searchMyTrackApplications(userId))
                        .thenReturn(List.of());

                List<TrackApplicationResponse> response =
                        trackService.getMyTrackApplications(userId);

                assertThat(response).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("트랙 등록 신청 상세 조회")
    class GetTrackApplication {

        @Test
        @DisplayName("신청자 본인이면 상세 조회 성공")
        void getTrackApplication_success_whenRequester() {
            Long userId = 1L;
            Long applicationId = 100L;

            User user = mock(User.class);
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

            TrackApplication application = TrackApplication.create(
                    userId,
                    TrackType.FREE_CREATION,
                    null,
                    null,
                    null,
                    "밤편지 AI 버전",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    210L,
                    null,
                    null
            );
            ReflectionTestUtils.setField(application, "id", applicationId);
            ReflectionTestUtils.setField(
                    application,
                    "createdAt",
                    LocalDateTime.of(2026, 4, 19, 21, 0, 0)
            );
            ReflectionTestUtils.setField(
                    application,
                    "updatedAt",
                    LocalDateTime.of(2026, 4, 19, 21, 0, 0)
            );

            when(trackApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

            TrackApplicationDetailResponse response =
                    trackService.getTrackApplication(userId, applicationId);

            assertThat(response.applicationId()).isEqualTo(applicationId);
            assertThat(response.requesterUserId()).isEqualTo(userId);
            assertThat(response.trackType()).isEqualTo(TrackType.FREE_CREATION);
            assertThat(response.artistId()).isNull();
            assertThat(response.albumId()).isNull();
            assertThat(response.trackNumber()).isNull();
            assertThat(response.title()).isEqualTo("밤편지 AI 버전");
            assertThat(response.genre()).isEqualTo("BALLAD");
            assertThat(response.audioUrl()).isEqualTo("https://example.com/audio.mp3");
            assertThat(response.durationSec()).isEqualTo(210L);
            assertThat(response.publishDelayDays()).isNull();
            assertThat(response.status()).isEqualTo(ApplicationStatus.PENDING);
        }

        @Test
        @DisplayName("관리자면 상세 조회 성공")
        void getTrackApplication_success_whenAdmin() {
            Long userId = 99L;
            Long applicationId = 100L;
            Long requesterUserId = 1L;

            User admin = mock(User.class);
            when(admin.getRole()).thenReturn(UserRole.ADMIN);
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(admin));

            TrackApplication application = TrackApplication.create(
                    requesterUserId,
                    TrackType.OFFICIAL_RELEASE,
                    10L,
                    100L,
                    1L,
                    "밤편지",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    230L,
                    "feat. 10cm",
                    3
            );
            ReflectionTestUtils.setField(application, "id", applicationId);
            ReflectionTestUtils.setField(
                    application,
                    "createdAt",
                    LocalDateTime.of(2026, 4, 19, 21, 0, 0)
            );
            ReflectionTestUtils.setField(
                    application,
                    "updatedAt",
                    LocalDateTime.of(2026, 4, 19, 21, 0, 0)
            );

            when(trackApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

            TrackApplicationDetailResponse response =
                    trackService.getTrackApplication(userId, applicationId);

            assertThat(response.applicationId()).isEqualTo(applicationId);
            assertThat(response.requesterUserId()).isEqualTo(requesterUserId);
            assertThat(response.trackType()).isEqualTo(TrackType.OFFICIAL_RELEASE);
            assertThat(response.artistId()).isEqualTo(10L);
            assertThat(response.albumId()).isEqualTo(100L);
            assertThat(response.trackNumber()).isEqualTo(1L);
            assertThat(response.title()).isEqualTo("밤편지");
            assertThat(response.featuredArtistText()).isEqualTo("feat. 10cm");
            assertThat(response.publishDelayDays()).isEqualTo(3);
        }

        @Test
        @DisplayName("본인도 관리자도 아니면 상세 조회 실패")
        void getTrackApplication_fail_whenForbidden() {
            Long userId = 2L;
            Long applicationId = 100L;

            User user = mock(User.class);
            when(user.getRole()).thenReturn(UserRole.USER);
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

            TrackApplication application = TrackApplication.create(
                    1L,
                    TrackType.FREE_CREATION,
                    null,
                    null,
                    null,
                    "밤편지 AI 버전",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    210L,
                    null,
                    null
            );
            ReflectionTestUtils.setField(application, "id", applicationId);

            when(trackApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

            assertThatThrownBy(() -> trackService.getTrackApplication(userId, applicationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackApplicationErrorCode.ERR_TRACK_APPLICATION_DETAIL_FORBIDDEN.getMessage());
        }

        @Test
        @DisplayName("존재하지 않는 신청이면 상세 조회 실패")
        void getTrackApplication_fail_whenNotFound() {
            Long userId = 1L;
            Long applicationId = 100L;

            when(trackApplicationRepository.findById(applicationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> trackService.getTrackApplication(userId, applicationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackApplicationErrorCode.ERR_TRACK_APPLICATION_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("트랙 등록 신청 목록 조회")
    class GetTrackApplications {

        @Test
        @DisplayName("상태 조건 없이 오래된순 목록 조회 성공")
        void getTrackApplications_success_withoutStatus() {
            Pageable pageable = PageRequest.of(0, 10);

            TrackApplication application1 = TrackApplication.create(
                    1L,
                    TrackType.FREE_CREATION,
                    null,
                    null,
                    null,
                    "첫 번째 신청",
                    "가사1",
                    "BALLAD",
                    "https://example.com/audio1.mp3",
                    210L,
                    null,
                    null
            );
            ReflectionTestUtils.setField(application1, "id", 1L);
            ReflectionTestUtils.setField(
                    application1,
                    "createdAt",
                    LocalDateTime.of(2026, 4, 14, 15, 0, 0)
            );

            TrackApplication application2 = TrackApplication.create(
                    2L,
                    TrackType.OFFICIAL_RELEASE,
                    10L,
                    100L,
                    1L,
                    "두 번째 신청",
                    "가사2",
                    "BALLAD",
                    "https://example.com/audio2.mp3",
                    230L,
                    "feat. 10cm",
                    3
            );
            ReflectionTestUtils.setField(application2, "id", 2L);
            ReflectionTestUtils.setField(
                    application2,
                    "createdAt",
                    LocalDateTime.of(2026, 4, 15, 15, 0, 0)
            );

            when(trackApplicationRepository.searchTrackApplications(null, pageable))
                    .thenReturn(new PageImpl<>(List.of(application1, application2), pageable, 2));

            PageResponse<TrackApplicationListResponse> response =
                    trackService.getTrackApplications(null, pageable);

            assertThat(response.content()).hasSize(2);
            assertThat(response.content().get(0).applicationId()).isEqualTo(1L);
            assertThat(response.content().get(0).requesterUserId()).isEqualTo(1L);
            assertThat(response.content().get(0).title()).isEqualTo("첫 번째 신청");
            assertThat(response.content().get(1).applicationId()).isEqualTo(2L);
            assertThat(response.page()).isEqualTo(0);
            assertThat(response.size()).isEqualTo(10);
            assertThat(response.totalElements()).isEqualTo(2);
            assertThat(response.totalPages()).isEqualTo(1);
        }

        @Test
        @DisplayName("상태 조건으로 목록 조회 성공")
        void getTrackApplications_success_withStatus() {
            Pageable pageable = PageRequest.of(0, 10);

            TrackApplication application = TrackApplication.create(
                    1L,
                    TrackType.FREE_CREATION,
                    null,
                    null,
                    null,
                    "승인 대기 신청",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    210L,
                    null,
                    null
            );
            ReflectionTestUtils.setField(application, "id", 1L);
            ReflectionTestUtils.setField(
                    application,
                    "createdAt",
                    LocalDateTime.of(2026, 4, 14, 15, 0, 0)
            );

            when(trackApplicationRepository.searchTrackApplications(ApplicationStatus.PENDING, pageable))
                    .thenReturn(new PageImpl<>(List.of(application), pageable, 1));

            PageResponse<TrackApplicationListResponse> response =
                    trackService.getTrackApplications(ApplicationStatus.PENDING, pageable);

            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).status()).isEqualTo(ApplicationStatus.PENDING);
            assertThat(response.totalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("조회 결과가 없으면 빈 페이지 조회 성공")
        void getTrackApplications_success_whenEmpty() {
            Pageable pageable = PageRequest.of(0, 10);

            when(trackApplicationRepository.searchTrackApplications(ApplicationStatus.REJECTED, pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            PageResponse<TrackApplicationListResponse> response =
                    trackService.getTrackApplications(ApplicationStatus.REJECTED, pageable);

            assertThat(response.content()).isEmpty();
            assertThat(response.totalElements()).isZero();
            assertThat(response.totalPages()).isZero();
        }
    }

    @Nested
    @DisplayName("트랙 등록 신청 승인")
    class ApproveTrackApplication {

        @Test
        @DisplayName("즉시 공개 신청이면 승인과 함께 트랙 생성 및 공개 성공")
        void approveTrackApplication_success_whenImmediatePublish() {
            Long adminId = 1L;
            Long applicationId = 10L;
            Long artistId = 100L;
            Long albumId = 200L;

            TrackApplication application = TrackApplication.create(
                    2L,
                    TrackType.OFFICIAL_RELEASE,
                    artistId,
                    albumId,
                    1L,
                    "밤편지",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    230L,
                    "feat. 10cm",
                    0
            );
            ReflectionTestUtils.setField(application, "id", applicationId);

            Artist artist = Artist.create(
                    application.getRequesterUserId(),
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);

            Album album = Album.create(
                    artistId,
                    "Palette",
                    "정규 앨범",
                    "https://example.com/cover.jpg",
                    null
            );
            ReflectionTestUtils.setField(album, "id", albumId);

            Track savedTrack = Track.createOfficialRelease(
                    application.getRequesterUserId(),
                    application.getArtistId(),
                    application.getAlbumId(),
                    application.getTrackNumber(),
                    application.getTitle(),
                    application.getLyrics(),
                    application.getGenre(),
                    application.getAudioUrl(),
                    application.getDurationSec(),
                    application.getFeaturedArtistText(),
                    null
            );
            savedTrack.publish();
            ReflectionTestUtils.setField(savedTrack, "id", 1000L);

            when(trackApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));
            when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));
            when(trackRepository.save(any(Track.class))).thenReturn(savedTrack);

            TrackApplicationApproveResponse response =
                    trackService.approveTrackApplication(adminId, applicationId);

            assertThat(response.applicationId()).isEqualTo(applicationId);
            assertThat(response.trackId()).isEqualTo(1000L);
            assertThat(response.status()).isEqualTo(ApplicationStatus.APPROVED);
            assertThat(response.reviewedByAdminId()).isEqualTo(adminId);
            assertThat(response.reviewedAt()).isNotNull();
        }

        @Test
        @DisplayName("예약 공개 신청이면 승인과 함께 트랙 생성 성공")
        void approveTrackApplication_success_whenScheduledPublish() {
            Long adminId = 1L;
            Long applicationId = 10L;
            Long artistId = 100L;
            Long albumId = 200L;

            TrackApplication application = TrackApplication.create(
                    2L,
                    TrackType.OFFICIAL_RELEASE,
                    artistId,
                    albumId,
                    1L,
                    "밤편지",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    230L,
                    "feat. 10cm",
                    3
            );
            ReflectionTestUtils.setField(application, "id", applicationId);

            Artist artist = Artist.create(
                    application.getRequesterUserId(),
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);

            Album album = Album.create(
                    artistId,
                    "Palette",
                    "정규 앨범",
                    "https://example.com/cover.jpg",
                    null
            );
            ReflectionTestUtils.setField(album, "id", albumId);

            Track savedTrack = Track.createOfficialRelease(
                    application.getRequesterUserId(),
                    application.getArtistId(),
                    application.getAlbumId(),
                    application.getTrackNumber(),
                    application.getTitle(),
                    application.getLyrics(),
                    application.getGenre(),
                    application.getAudioUrl(),
                    application.getDurationSec(),
                    application.getFeaturedArtistText(),
                    LocalDateTime.now().plusDays(3)
            );
            ReflectionTestUtils.setField(savedTrack, "id", 1000L);

            when(trackApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));
            when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));
            when(trackRepository.save(any(Track.class))).thenReturn(savedTrack);

            TrackApplicationApproveResponse response =
                    trackService.approveTrackApplication(adminId, applicationId);

            assertThat(response.applicationId()).isEqualTo(applicationId);
            assertThat(response.trackId()).isEqualTo(1000L);
            assertThat(response.status()).isEqualTo(ApplicationStatus.APPROVED);
            assertThat(response.reviewedByAdminId()).isEqualTo(adminId);
            assertThat(response.reviewedAt()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 신청이면 승인 실패")
        void approveTrackApplication_fail_whenNotFound() {
            Long adminId = 1L;
            Long applicationId = 10L;

            when(trackApplicationRepository.findById(applicationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> trackService.approveTrackApplication(adminId, applicationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackApplicationErrorCode.ERR_TRACK_APPLICATION_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("이미 처리된 신청이면 승인 실패")
        void approveTrackApplication_fail_whenAlreadyProcessed() {
            Long adminId = 1L;
            Long applicationId = 10L;
            Long artistId = 100L;
            Long albumId = 200L;

            TrackApplication application = TrackApplication.create(
                    2L,
                    TrackType.OFFICIAL_RELEASE,
                    artistId,
                    albumId,
                    1L,
                    "밤편지",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    230L,
                    "feat. 10cm",
                    0
            );
            ReflectionTestUtils.setField(application, "id", applicationId);
            application.approve(adminId);

            Artist artist = Artist.create(
                    application.getRequesterUserId(),
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);

            Album album = Album.create(
                    artistId,
                    "Palette",
                    "정규 앨범",
                    "https://example.com/cover.jpg",
                    null
            );
            ReflectionTestUtils.setField(album, "id", albumId);

            when(trackApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));
            when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));

            assertThatThrownBy(() -> trackService.approveTrackApplication(adminId, applicationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackApplicationErrorCode.ERR_TRACK_APPLICATION_ALREADY_PROCESSED.getMessage());
        }

        @Test
        @DisplayName("자유 창작 신청이면 승인과 함께 트랙 생성 및 공개 성공")
        void approveTrackApplication_success_whenFreeCreation() {
            Long adminId = 1L;
            Long applicationId = 10L;

            TrackApplication application = TrackApplication.create(
                    2L,
                    TrackType.FREE_CREATION,
                    null,
                    null,
                    null,
                    "밤편지 AI 버전",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    210L,
                    null,
                    null
            );
            ReflectionTestUtils.setField(application, "id", applicationId);

            Track savedTrack = Track.createFreeCreation(
                    application.getRequesterUserId(),
                    application.getTitle(),
                    application.getLyrics(),
                    application.getGenre(),
                    application.getAudioUrl(),
                    application.getDurationSec()
            );
            ReflectionTestUtils.setField(savedTrack, "id", 1000L);

            when(trackApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(trackRepository.save(any(Track.class))).thenReturn(savedTrack);

            TrackApplicationApproveResponse response =
                    trackService.approveTrackApplication(adminId, applicationId);

            assertThat(response.applicationId()).isEqualTo(applicationId);
            assertThat(response.trackId()).isEqualTo(1000L);
            assertThat(response.status()).isEqualTo(ApplicationStatus.APPROVED);
            assertThat(response.reviewedByAdminId()).isEqualTo(adminId);
            assertThat(response.reviewedAt()).isNotNull();
        }

        @Test
        @DisplayName("승인 시점에 삭제된 아티스트면 승인 실패")
        void approveTrackApplication_fail_whenArtistDeletedAtApproval() {
            Long adminId = 1L;
            Long applicationId = 10L;
            Long artistId = 100L;
            Long albumId = 200L;

            TrackApplication application = TrackApplication.create(
                    2L,
                    TrackType.OFFICIAL_RELEASE,
                    artistId,
                    albumId,
                    1L,
                    "밤편지",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    230L,
                    "feat. 10cm",
                    0
            );
            ReflectionTestUtils.setField(application, "id", applicationId);

            Artist artist = Artist.create(
                    application.getRequesterUserId(),
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);
            ReflectionTestUtils.setField(
                    artist,
                    "deletedAt",
                    LocalDateTime.of(2026, 4, 20, 12, 0, 0)
            );

            when(trackApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));

            assertThatThrownBy(() -> trackService.approveTrackApplication(adminId, applicationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_ARTIST_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("승인 시점에 비활성화된 아티스트면 승인 실패")
        void approveTrackApplication_fail_whenArtistInactiveAtApproval() {
            Long adminId = 1L;
            Long applicationId = 10L;
            Long artistId = 100L;
            Long albumId = 200L;

            TrackApplication application = TrackApplication.create(
                    2L,
                    TrackType.OFFICIAL_RELEASE,
                    artistId,
                    albumId,
                    1L,
                    "밤편지",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    230L,
                    "feat. 10cm",
                    0
            );
            ReflectionTestUtils.setField(application, "id", applicationId);

            Artist artist = Artist.create(
                    application.getRequesterUserId(),
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);
            artist.deactivate();

            when(trackApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));

            assertThatThrownBy(() -> trackService.approveTrackApplication(adminId, applicationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackApplicationErrorCode.ERR_INACTIVE_ARTIST_CANNOT_REQUEST_OFFICIAL_RELEASE.getMessage());
        }

        @Test
        @DisplayName("승인 시점에 삭제된 앨범이면 승인 실패")
        void approveTrackApplication_fail_whenAlbumDeletedAtApproval() {
            Long adminId = 1L;
            Long applicationId = 10L;
            Long artistId = 100L;
            Long albumId = 200L;

            TrackApplication application = TrackApplication.create(
                    2L,
                    TrackType.OFFICIAL_RELEASE,
                    artistId,
                    albumId,
                    1L,
                    "밤편지",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    230L,
                    "feat. 10cm",
                    0
            );
            ReflectionTestUtils.setField(application, "id", applicationId);

            Artist artist = Artist.create(
                    application.getRequesterUserId(),
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);

            Album album = Album.create(
                    artistId,
                    "Palette",
                    "정규 앨범",
                    "https://example.com/cover.jpg",
                    null
            );
            ReflectionTestUtils.setField(album, "id", albumId);
            ReflectionTestUtils.setField(
                    album,
                    "deletedAt",
                    LocalDateTime.of(2026, 4, 20, 12, 0, 0)
            );

            when(trackApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));
            when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));

            assertThatThrownBy(() -> trackService.approveTrackApplication(adminId, applicationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AlbumErrorCode.ERR_ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("승인 시점에 앨범과 아티스트 정보가 일치하지 않으면 승인 실패")
        void approveTrackApplication_fail_whenAlbumArtistMismatchAtApproval() {
            Long adminId = 1L;
            Long applicationId = 10L;
            Long artistId = 100L;
            Long albumId = 200L;

            TrackApplication application = TrackApplication.create(
                    2L,
                    TrackType.OFFICIAL_RELEASE,
                    artistId,
                    albumId,
                    1L,
                    "밤편지",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    230L,
                    "feat. 10cm",
                    0
            );
            ReflectionTestUtils.setField(application, "id", applicationId);

            Artist artist = Artist.create(
                    application.getRequesterUserId(),
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);

            Album album = Album.create(
                    999L,
                    "Palette",
                    "정규 앨범",
                    "https://example.com/cover.jpg",
                    null
            );
            ReflectionTestUtils.setField(album, "id", albumId);

            when(trackApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));
            when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));

            assertThatThrownBy(() -> trackService.approveTrackApplication(adminId, applicationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackApplicationErrorCode.ERR_ALBUM_ARTIST_MISMATCH.getMessage());
        }
    }

    @Nested
    @DisplayName("트랙 등록 신청 거절")
    class RejectTrackApplication {

        @Test
        @DisplayName("거절 성공")
        void rejectTrackApplication_success() {
            Long adminId = 1L;
            Long applicationId = 10L;
            String rejectionReason = "트랙 정보가 부족합니다";

            TrackApplication application = TrackApplication.create(
                    2L,
                    TrackType.OFFICIAL_RELEASE,
                    100L,
                    200L,
                    1L,
                    "밤편지",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    230L,
                    "feat. 10cm",
                    3
            );
            ReflectionTestUtils.setField(application, "id", applicationId);

            when(trackApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

            TrackApplicationRejectResponse response =
                    trackService.rejectTrackApplication(adminId, applicationId, rejectionReason);

            assertThat(response.applicationId()).isEqualTo(applicationId);
            assertThat(response.status()).isEqualTo(ApplicationStatus.REJECTED);
            assertThat(response.reviewedByAdminId()).isEqualTo(adminId);
            assertThat(response.reviewedAt()).isNotNull();
            assertThat(response.rejectionReason()).isEqualTo(rejectionReason);
        }

        @Test
        @DisplayName("존재하지 않는 신청이면 거절 실패")
        void rejectTrackApplication_fail_whenNotFound() {
            Long adminId = 1L;
            Long applicationId = 10L;

            when(trackApplicationRepository.findById(applicationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    trackService.rejectTrackApplication(adminId, applicationId, "사유")
            )
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackApplicationErrorCode.ERR_TRACK_APPLICATION_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("이미 처리된 신청이면 거절 실패")
        void rejectTrackApplication_fail_whenAlreadyProcessed() {
            Long adminId = 1L;
            Long applicationId = 10L;

            TrackApplication application = TrackApplication.create(
                    2L,
                    TrackType.OFFICIAL_RELEASE,
                    100L,
                    200L,
                    1L,
                    "밤편지",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    230L,
                    "feat. 10cm",
                    0
            );
            ReflectionTestUtils.setField(application, "id", applicationId);
            application.approve(adminId);

            when(trackApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

            assertThatThrownBy(() ->
                    trackService.rejectTrackApplication(adminId, applicationId, "사유")
            )
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackApplicationErrorCode.ERR_TRACK_APPLICATION_ALREADY_PROCESSED.getMessage());
        }
    }

    @Nested
    @DisplayName("트랙 상세 조회")
    class GetTrack {

        @Test
        @DisplayName("트랙 상세 조회 성공")
        void getTrack_success() {
            Long trackId = 1L;
            Long artistId = 10L;
            Long albumId = 100L;

            TrackComment comment = mock(TrackComment.class);
            when(comment.getId()).thenReturn(10L);
            when(comment.getUserId()).thenReturn(3L);
            when(comment.getTrackId()).thenReturn(trackId);
            when(comment.getContent()).thenReturn("좋아요");
            when(comment.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 4, 22, 13, 0, 0));
            when(comment.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 4, 22, 13, 0, 0));

            when(trackCommentRepository.getRecentTrackComments(trackId, 5))
                    .thenReturn(List.of(comment));

            Track track = Track.createOfficialRelease(
                    1L,
                    artistId,
                    albumId,
                    1L,
                    "밤편지",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    230L,
                    "feat. 10cm",
                    null
            );
            ReflectionTestUtils.setField(track, "id", trackId);
            ReflectionTestUtils.setField(track, "status", TrackStatus.PUBLISHED);
            ReflectionTestUtils.setField(
                    track,
                    "publishedAt",
                    LocalDateTime.of(2026, 5, 1, 18, 0, 0)
            );
            ReflectionTestUtils.setField(track, "playCount", 1200L);

            when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));
            Album album = Album.create(
                    artistId,
                    "Palette",
                    "정규 앨범",
                    "https://example.com/cover.jpg",
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

            when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));
            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));
            when(trackRepository.findTrackDetailById(trackId))
                    .thenReturn(new TrackDetailProjection("아이유", "Palette"));
            stubTrackDetailCacheMiss(trackId);

            TrackDetailResponse response = trackService.getTrack(trackId);

            assertThat(response.trackId()).isEqualTo(trackId);
            assertThat(response.trackType()).isEqualTo(TrackType.OFFICIAL_RELEASE);
            assertThat(response.artistId()).isEqualTo(artistId);
            assertThat(response.artistName()).isEqualTo("아이유");
            assertThat(response.albumId()).isEqualTo(albumId);
            assertThat(response.albumTitle()).isEqualTo("Palette");
            assertThat(response.trackNumber()).isEqualTo(1L);
            assertThat(response.title()).isEqualTo("밤편지");
            assertThat(response.lyrics()).isEqualTo("가사");
            assertThat(response.genre()).isEqualTo("BALLAD");
            assertThat(response.audioUrl()).isEqualTo("https://example.com/audio.mp3");
            assertThat(response.durationSec()).isEqualTo(230L);
            assertThat(response.featuredArtistText()).isEqualTo("feat. 10cm");
            assertThat(response.playCount()).isEqualTo(1200L);
            assertThat(response.publishedAt()).isEqualTo(LocalDateTime.of(2026, 5, 1, 18, 0, 0));
            assertThat(response.comments()).hasSize(1);
            assertThat(response.comments().get(0).commentId()).isEqualTo(10L);
            assertThat(response.comments().get(0).userId()).isEqualTo(3L);
            assertThat(response.comments().get(0).trackId()).isEqualTo(trackId);
            assertThat(response.comments().get(0).content()).isEqualTo("좋아요");
        }

        @Test
        @DisplayName("자유 창작 트랙 상세 조회 성공")
        void getTrack_success_whenFreeCreation() {
            Long trackId = 2L;

            Track track = Track.createFreeCreation(
                    1L,
                    "밤편지 AI 버전",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio-free.mp3",
                    210L
            );
            ReflectionTestUtils.setField(track, "id", trackId);
            ReflectionTestUtils.setField(
                    track,
                    "publishedAt",
                    LocalDateTime.of(2026, 5, 2, 12, 0, 0)
            );
            ReflectionTestUtils.setField(track, "playCount", 300L);

            when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));
            when(trackRepository.findTrackDetailById(trackId)).thenReturn(null);
            stubTrackDetailCacheMiss(trackId);

            TrackDetailResponse response = trackService.getTrack(trackId);

            assertThat(response.trackId()).isEqualTo(trackId);
            assertThat(response.trackType()).isEqualTo(TrackType.FREE_CREATION);
            assertThat(response.artistId()).isNull();
            assertThat(response.artistName()).isNull();
            assertThat(response.albumId()).isNull();
            assertThat(response.albumTitle()).isNull();
            assertThat(response.trackNumber()).isNull();
            assertThat(response.title()).isEqualTo("밤편지 AI 버전");
            assertThat(response.audioUrl()).isEqualTo("https://example.com/audio-free.mp3");
            assertThat(response.durationSec()).isEqualTo(210L);
            assertThat(response.playCount()).isEqualTo(300L);
            assertThat(response.publishedAt()).isEqualTo(LocalDateTime.of(2026, 5, 2, 12, 0, 0));
        }

        @Test
        @DisplayName("존재하지 않는 트랙이면 상세 조회 실패")
        void getTrack_fail_whenNotFound() {
            Long trackId = 1L;

            when(trackRepository.findById(trackId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> trackService.getTrack(trackId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackErrorCode.ERR_TRACK_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("삭제된 트랙이면 상세 조회 실패")
        void getTrack_fail_whenDeleted() {
            Long trackId = 1L;

            Track track = Track.createFreeCreation(
                    1L,
                    "밤편지 AI 버전",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    210L
            );
            ReflectionTestUtils.setField(track, "id", trackId);
            ReflectionTestUtils.setField(
                    track,
                    "deletedAt",
                    LocalDateTime.of(2026, 4, 20, 12, 0, 0)
            );

            when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));

            assertThatThrownBy(() -> trackService.getTrack(trackId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackErrorCode.ERR_TRACK_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("비공개 트랙이면 상세 조회 실패")
        void getTrack_fail_whenNotPublished() {
            Long trackId = 1L;

            Track track = Track.createOfficialRelease(
                    1L,
                    10L,
                    100L,
                    1L,
                    "밤편지",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    230L,
                    "feat. 10cm",
                    null
            );
            ReflectionTestUtils.setField(track, "id", trackId);

            when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));

            assertThatThrownBy(() -> trackService.getTrack(trackId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackErrorCode.ERR_TRACK_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("정식 발매 트랙 상세 조회 시 삭제된 앨범이면 실패")
        void getTrack_fail_whenOfficialTrackAlbumDeleted() {
            Long trackId = 1L;
            Long artistId = 10L;
            Long albumId = 100L;

            Track track = Track.createOfficialRelease(
                    1L,
                    artistId,
                    albumId,
                    1L,
                    "밤편지",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    230L,
                    "feat. 10cm",
                    null
            );
            track.publish();
            ReflectionTestUtils.setField(track, "id", trackId);

            Album album = Album.create(
                    artistId,
                    "Palette",
                    "정규 앨범",
                    "https://example.com/cover.jpg",
                    null
            );
            ReflectionTestUtils.setField(album, "id", albumId);
            ReflectionTestUtils.setField(
                    album,
                    "deletedAt",
                    LocalDateTime.of(2026, 5, 2, 12, 0, 0)
            );

            when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));
            when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));

            assertThatThrownBy(() -> trackService.getTrack(trackId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackErrorCode.ERR_TRACK_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("정식 발매 트랙 상세 조회 시 미공개 앨범이면 실패")
        void getTrack_fail_whenOfficialTrackAlbumNotPublished() {
            Long trackId = 1L;
            Long artistId = 10L;
            Long albumId = 100L;

            Track track = Track.createOfficialRelease(
                    1L,
                    artistId,
                    albumId,
                    1L,
                    "밤편지",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    230L,
                    "feat. 10cm",
                    null
            );
            track.publish();
            ReflectionTestUtils.setField(track, "id", trackId);

            Album album = Album.create(
                    artistId,
                    "Palette",
                    "정규 앨범",
                    "https://example.com/cover.jpg",
                    null
            );
            ReflectionTestUtils.setField(album, "id", albumId);

            when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));
            when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));

            assertThatThrownBy(() -> trackService.getTrack(trackId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackErrorCode.ERR_TRACK_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("정식 발매 트랙 상세 조회 시 삭제된 아티스트면 실패")
        void getTrack_fail_whenOfficialTrackArtistDeleted() {
            Long trackId = 1L;
            Long artistId = 10L;
            Long albumId = 100L;

            Track track = Track.createOfficialRelease(
                    1L,
                    artistId,
                    albumId,
                    1L,
                    "밤편지",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    230L,
                    "feat. 10cm",
                    null
            );
            track.publish();
            ReflectionTestUtils.setField(track, "id", trackId);

            Album album = Album.create(
                    artistId,
                    "Palette",
                    "정규 앨범",
                    "https://example.com/cover.jpg",
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
            ReflectionTestUtils.setField(
                    artist,
                    "deletedAt",
                    LocalDateTime.of(2026, 5, 2, 12, 0, 0)
            );

            when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));
            when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));
            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));

            assertThatThrownBy(() -> trackService.getTrack(trackId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackErrorCode.ERR_TRACK_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("캐시가 있으면 트랙 상세 핵심 정보는 캐시를 사용하고 댓글과 재생 수는 실시간 조회")
        void getTrack_success_whenCacheHit() {
            Long trackId = 1L;

            Track track = Track.createFreeCreation(
                    1L,
                    "캐시 이전 제목",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    210L
            );
            ReflectionTestUtils.setField(track, "id", trackId);
            ReflectionTestUtils.setField(track, "playCount", 999L);
            ReflectionTestUtils.setField(
                    track,
                    "publishedAt",
                    LocalDateTime.of(2026, 5, 1, 18, 0, 0)
            );

            TrackDetailCache cache = new TrackDetailCache(
                    trackId,
                    TrackType.FREE_CREATION,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "캐시된 제목",
                    "캐시된 가사",
                    "BALLAD",
                    "https://example.com/cached-audio.mp3",
                    200L,
                    null,
                    LocalDateTime.of(2026, 5, 1, 18, 0, 0)
            );

            TrackComment comment = mock(TrackComment.class);
            when(comment.getId()).thenReturn(10L);
            when(comment.getUserId()).thenReturn(3L);
            when(comment.getTrackId()).thenReturn(trackId);
            when(comment.getContent()).thenReturn("실시간 댓글");
            when(comment.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 5, 2, 10, 0, 0));
            when(comment.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 5, 2, 10, 0, 0));

            when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));
            when(trackDetailCacheService.getOrLoad(eq(trackId), any()))
                    .thenReturn(cache);
            when(trackCommentRepository.getRecentTrackComments(trackId, 5))
                    .thenReturn(List.of(comment));

            TrackDetailResponse response = trackService.getTrack(trackId);

            assertThat(response.trackId()).isEqualTo(trackId);
            assertThat(response.title()).isEqualTo("캐시된 제목");
            assertThat(response.lyrics()).isEqualTo("캐시된 가사");
            assertThat(response.audioUrl()).isEqualTo("https://example.com/cached-audio.mp3");
            assertThat(response.durationSec()).isEqualTo(200L);
            assertThat(response.playCount()).isEqualTo(999L);
            assertThat(response.comments()).hasSize(1);
            assertThat(response.comments().get(0).content()).isEqualTo("실시간 댓글");

            verify(trackRepository, never()).findTrackDetailById(trackId);
            verify(albumRepository, never()).findById(any());
            verify(artistRepository, never()).findById(any());
        }

        @Test
        @DisplayName("캐시가 있어도 정식 발매 트랙의 앨범이 삭제되면 상세 조회 실패")
        void getTrack_fail_whenCacheHitAndAlbumDeleted() {
            Long trackId = 1L;
            Long artistId = 10L;
            Long albumId = 100L;

            Track track = Track.createOfficialRelease(
                    1L,
                    artistId,
                    albumId,
                    1L,
                    "밤편지",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    230L,
                    "feat. 10cm",
                    null
            );
            track.publish();
            ReflectionTestUtils.setField(track, "id", trackId);

            Album album = Album.create(
                    artistId,
                    "Palette",
                    "정규 앨범",
                    "https://example.com/cover.jpg",
                    null
            );
            ReflectionTestUtils.setField(album, "id", albumId);
            ReflectionTestUtils.setField(
                    album,
                    "deletedAt",
                    LocalDateTime.of(2026, 5, 2, 12, 0, 0)
            );

            when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));
            when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));

            assertThatThrownBy(() -> trackService.getTrack(trackId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackErrorCode.ERR_TRACK_NOT_FOUND.getMessage());

            verify(trackDetailCacheService, never()).getOrLoad(any(), any());
        }
    }

    @Nested
    @DisplayName("공개 트랙 목록 조회")
    class GetPublicTracks {

        @Test
        @DisplayName("공개 트랙 목록 조회 성공")
        void getPublicTracks_success() {
            Pageable pageable = PageRequest.of(0, 20);

            PublicTrackListProjection projection1 = new PublicTrackListProjection(
                    1L,
                    TrackType.OFFICIAL_RELEASE,
                    "밤편지",
                    10L,
                    "아이유",
                    100L,
                    "Palette",
                    230L,
                    1200L,
                    LocalDateTime.of(2026, 5, 1, 18, 0, 0)
            );

            PublicTrackListProjection projection2 = new PublicTrackListProjection(
                    2L,
                    TrackType.OFFICIAL_RELEASE,
                    "이름에게",
                    10L,
                    "아이유",
                    101L,
                    "Love poem",
                    245L,
                    980L,
                    LocalDateTime.of(2026, 4, 30, 18, 0, 0)
            );

            when(trackRepository.searchPublicTracks(pageable))
                    .thenReturn(new SliceImpl<>(List.of(projection1, projection2), pageable, true));

            SliceResponse<PublicTrackListResponse> response =
                    trackService.getPublicTracks(pageable);

            assertThat(response.content()).hasSize(2);

            assertThat(response.content().get(0).trackId()).isEqualTo(1L);
            assertThat(response.content().get(0).trackType()).isEqualTo(TrackType.OFFICIAL_RELEASE);
            assertThat(response.content().get(0).title()).isEqualTo("밤편지");
            assertThat(response.content().get(0).artistId()).isEqualTo(10L);
            assertThat(response.content().get(0).artistName()).isEqualTo("아이유");
            assertThat(response.content().get(0).albumId()).isEqualTo(100L);
            assertThat(response.content().get(0).albumTitle()).isEqualTo("Palette");
            assertThat(response.content().get(0).durationSec()).isEqualTo(230L);
            assertThat(response.content().get(0).playCount()).isEqualTo(1200L);
            assertThat(response.content().get(0).publishedAt())
                    .isEqualTo(LocalDateTime.of(2026, 5, 1, 18, 0, 0));

            assertThat(response.page()).isEqualTo(0);
            assertThat(response.size()).isEqualTo(20);
            assertThat(response.hasNext()).isTrue();
        }

        @Test
        @DisplayName("자유 창작 트랙이면 연관 정보 없이 공개 트랙 목록 조회 성공")
        void getPublicTracks_success_whenFreeCreation() {
            Pageable pageable = PageRequest.of(0, 20);

            PublicTrackListProjection projection = new PublicTrackListProjection(
                    3L,
                    TrackType.FREE_CREATION,
                    "밤편지 AI 버전",
                    null,
                    null,
                    null,
                    null,
                    210L,
                    300L,
                    LocalDateTime.of(2026, 5, 2, 12, 0, 0)
            );

            when(trackRepository.searchPublicTracks(pageable))
                    .thenReturn(new SliceImpl<>(List.of(projection), pageable, false));

            SliceResponse<PublicTrackListResponse> response =
                    trackService.getPublicTracks(pageable);

            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).trackId()).isEqualTo(3L);
            assertThat(response.content().get(0).trackType()).isEqualTo(TrackType.FREE_CREATION);
            assertThat(response.content().get(0).title()).isEqualTo("밤편지 AI 버전");
            assertThat(response.content().get(0).artistId()).isNull();
            assertThat(response.content().get(0).artistName()).isNull();
            assertThat(response.content().get(0).albumId()).isNull();
            assertThat(response.content().get(0).albumTitle()).isNull();
            assertThat(response.content().get(0).durationSec()).isEqualTo(210L);
            assertThat(response.content().get(0).playCount()).isEqualTo(300L);
            assertThat(response.content().get(0).publishedAt())
                    .isEqualTo(LocalDateTime.of(2026, 5, 2, 12, 0, 0));
            assertThat(response.hasNext()).isFalse();
        }

        @Test
        @DisplayName("조회 결과가 없으면 빈 페이지 조회 성공")
        void getPublicTracks_success_whenEmpty() {
            Pageable pageable = PageRequest.of(0, 20);

            when(trackRepository.searchPublicTracks(pageable))
                    .thenReturn(new SliceImpl<>(List.of(), pageable, false));

            SliceResponse<PublicTrackListResponse> response =
                    trackService.getPublicTracks(pageable);

            assertThat(response.content()).isEmpty();
            assertThat(response.page()).isEqualTo(0);
            assertThat(response.size()).isEqualTo(20);
            assertThat(response.hasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("아티스트별 자유 창작 트랙 목록 조회")
    class GetArtistFreeCreations {

        @Test
        @DisplayName("아티스트의 자유 창작 트랙 목록을 조회한다")
        void getArtistFreeCreations_success() {

            // given
            Long artistId = 1L;
            Long ownerUserId = 10L;

            Pageable pageable = PageRequest.of(0, 20);

            Artist artist = mock(Artist.class);
            when(artist.getOwnerUserId()).thenReturn(ownerUserId);

            Track track = mock(Track.class);
            ReflectionTestUtils.setField(track, "id", 100L);
            when(track.getTitle()).thenReturn("자유 창작 트랙");
            when(track.getDurationSec()).thenReturn(200L);
            when(track.getPublishedAt()).thenReturn(LocalDateTime.now());

            Page<Track> page = new PageImpl<>(List.of(track));

            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));
            when(trackRepository.searchArtistFreeCreations(ownerUserId, pageable)).thenReturn(page);

            // when
            PageResponse<ArtistFreeCreationTrackResponse> response =
                    trackService.getArtistFreeCreations(artistId, pageable);

            // then
            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).title()).isEqualTo("자유 창작 트랙");
        }

        @Test
        @DisplayName("아티스트가 존재하지 않으면 예외가 발생한다")
        void getArtistFreeCreations_artist_not_found() {

            // given
            Long artistId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            when(artistRepository.findById(artistId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    trackService.getArtistFreeCreations(artistId, pageable)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_ARTIST_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("삭제된 아티스트면 예외가 발생한다")
        void getArtistFreeCreations_fail_whenArtistDeleted() {
            Long artistId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            Artist artist = Artist.create(
                    10L,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);
            ReflectionTestUtils.setField(
                    artist,
                    "deletedAt",
                    LocalDateTime.of(2026, 5, 1, 10, 0, 0)
            );

            when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));

            assertThatThrownBy(() ->
                    trackService.getArtistFreeCreations(artistId, pageable)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_ARTIST_NOT_FOUND.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void stubTrackDetailCacheMiss(Long trackId) {
        when(trackDetailCacheService.getOrLoad(eq(trackId), any()))
                .thenAnswer(invocation -> {
                    java.util.function.Supplier<TrackDetailCache> loader = invocation.getArgument(1);
                    return loader.get();
                });
    }
}