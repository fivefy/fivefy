package com.fivefy.domain.track.service;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.album.entity.Album;
import com.fivefy.domain.album.enums.AlbumErrorCode;
import com.fivefy.domain.album.repository.AlbumRepository;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.artist.enums.ArtistErrorCode;
import com.fivefy.domain.artist.enums.ArtistType;
import com.fivefy.domain.artist.repository.ArtistRepository;
import com.fivefy.domain.track.dto.request.FreeTrackApplicationCreateRequest;
import com.fivefy.domain.track.dto.request.OfficialTrackApplicationCreateRequest;
import com.fivefy.domain.track.dto.response.TrackApplicationResponse;
import com.fivefy.domain.track.entity.TrackApplication;
import com.fivefy.domain.track.enums.TrackApplicationErrorCode;
import com.fivefy.domain.track.enums.TrackType;
import com.fivefy.domain.track.repository.TrackApplicationRepository;
import com.fivefy.domain.track.repository.TrackRepository;
import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.enums.UserErrorCode;
import com.fivefy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TrackService의 비즈니스 로직을 검증하는 단위 테스트
 *
 * 자유 창작 트랙 등록 신청 생성 기능을 검증한다.
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
    }
}