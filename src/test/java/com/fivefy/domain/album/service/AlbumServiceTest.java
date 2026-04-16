package com.fivefy.domain.album.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.album.dto.request.AlbumReleaseRequestCreateRequest;
import com.fivefy.domain.album.dto.response.AlbumReleaseRequestCreateResponse;
import com.fivefy.domain.album.entity.AlbumReleaseRequest;
import com.fivefy.domain.album.enums.AlbumReleaseErrorCode;
import com.fivefy.domain.album.repository.AlbumReleaseRequestRepository;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.artist.enums.ArtistErrorCode;
import com.fivefy.domain.artist.enums.ArtistType;
import com.fivefy.domain.artist.repository.ArtistRepository;
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

import static com.fivefy.common.enums.ApplicationStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AlbumService의 비즈니스 로직을 검증하는 단위 테스트
 *
 * 앨범 등록 요청 생성 기능을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AlbumServiceTest {

    @Mock
    private AlbumReleaseRequestRepository albumReleaseRequestRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AlbumService albumService;

    @Nested
    @DisplayName("앨범 등록 요청 생성")
    class CreateAlbumReleaseRequest {

        @Test
        @DisplayName("앨범 등록 요청 생성에 성공한다")
        void createAlbumReleaseRequest_success() {
            // given
            Long userId = 1L;
            Long artistId = 10L;

            AlbumReleaseRequestCreateRequest request = new AlbumReleaseRequestCreateRequest(
                    artistId,
                    "Love poem",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    3
            );

            User user = mock(User.class);
            when(userRepository.findById(userId))
                    .thenReturn(java.util.Optional.of(user));

            Artist artist = Artist.create(
                    userId,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);

            when(artistRepository.findById(artistId))
                    .thenReturn(java.util.Optional.of(artist));

            when(albumReleaseRequestRepository.existsPendingRequest(
                    userId,
                    artistId,
                    request.title()
            )).thenReturn(false);

            AlbumReleaseRequest savedRequest = AlbumReleaseRequest.create(
                    userId,
                    artistId,
                    request.title(),
                    request.description(),
                    request.coverImageUrl(),
                    request.publishDelayDays()
            );

            ReflectionTestUtils.setField(savedRequest, "id", 1L);
            ReflectionTestUtils.setField(
                    savedRequest,
                    "createdAt",
                    LocalDateTime.of(2026, 4, 16, 16, 0, 0)
            );

            when(albumReleaseRequestRepository.save(any(AlbumReleaseRequest.class)))
                    .thenReturn(savedRequest);

            // when
            AlbumReleaseRequestCreateResponse response =
                    albumService.createAlbumReleaseRequest(userId, request);

            // then
            assertThat(response.requestId()).isEqualTo(1L);
            assertThat(response.artistId()).isEqualTo(artistId);
            assertThat(response.title()).isEqualTo("Love poem");
            assertThat(response.status()).isEqualTo(PENDING);
            assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 4, 16, 16, 0, 0));

            verify(userRepository, times(1)).findById(userId);
            verify(artistRepository, times(1)).findById(artistId);
            verify(albumReleaseRequestRepository, times(1))
                    .existsPendingRequest(userId, artistId, request.title());
            verify(albumReleaseRequestRepository, times(1))
                    .save(any(AlbumReleaseRequest.class));
        }

        @Test
        @DisplayName("존재하지 않는 유저면 앨범 등록 요청 생성에 실패한다")
        void createAlbumReleaseRequest_fail_whenUserNotFound() {
            // given
            Long userId = 1L;

            AlbumReleaseRequestCreateRequest request = new AlbumReleaseRequestCreateRequest(
                    10L,
                    "Love poem",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    0
            );

            when(userRepository.findById(userId))
                    .thenReturn(java.util.Optional.empty());

            // when & then
            assertThatThrownBy(() -> albumService.createAlbumReleaseRequest(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(UserErrorCode.ERR_USER_NOT_FOUND.getMessage());

            verify(userRepository, times(1)).findById(userId);
            verify(artistRepository, never()).findById(any());
            verify(albumReleaseRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 아티스트면 앨범 등록 요청 생성에 실패한다")
        void createAlbumReleaseRequest_fail_whenArtistNotFound() {
            // given
            Long userId = 1L;
            Long artistId = 10L;

            AlbumReleaseRequestCreateRequest request = new AlbumReleaseRequestCreateRequest(
                    artistId,
                    "Love poem",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    0
            );

            User user = mock(User.class);
            when(userRepository.findById(userId))
                    .thenReturn(java.util.Optional.of(user));

            when(artistRepository.findById(artistId))
                    .thenReturn(java.util.Optional.empty());

            // when & then
            assertThatThrownBy(() -> albumService.createAlbumReleaseRequest(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_ARTIST_NOT_FOUND.getMessage());

            verify(userRepository, times(1)).findById(userId);
            verify(artistRepository, times(1)).findById(artistId);
            verify(albumReleaseRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("삭제된 아티스트면 앨범 등록 요청 생성에 실패한다")
        void createAlbumReleaseRequest_fail_whenArtistDeleted() {
            // given
            Long userId = 1L;
            Long artistId = 10L;

            AlbumReleaseRequestCreateRequest request = new AlbumReleaseRequestCreateRequest(
                    artistId,
                    "Love poem",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    0
            );

            User user = mock(User.class);
            when(userRepository.findById(userId))
                    .thenReturn(java.util.Optional.of(user));

            Artist artist = Artist.create(
                    userId,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);
            ReflectionTestUtils.setField(
                    artist,
                    "deletedAt",
                    LocalDateTime.of(2026, 4, 16, 12, 0, 0)
            );

            when(artistRepository.findById(artistId))
                    .thenReturn(java.util.Optional.of(artist));

            // when & then
            assertThatThrownBy(() -> albumService.createAlbumReleaseRequest(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_ARTIST_NOT_FOUND.getMessage());

            verify(userRepository, times(1)).findById(userId);
            verify(artistRepository, times(1)).findById(artistId);
            verify(albumReleaseRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("아티스트 소유자가 아니면 앨범 등록 요청 생성에 실패한다")
        void createAlbumReleaseRequest_fail_whenNotOwner() {
            // given
            Long userId = 1L;
            Long artistId = 10L;

            AlbumReleaseRequestCreateRequest request = new AlbumReleaseRequestCreateRequest(
                    artistId,
                    "Love poem",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    0
            );

            User user = mock(User.class);
            when(userRepository.findById(userId))
                    .thenReturn(java.util.Optional.of(user));

            Artist artist = Artist.create(
                    2L,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);

            when(artistRepository.findById(artistId))
                    .thenReturn(java.util.Optional.of(artist));

            // when & then
            assertThatThrownBy(() -> albumService.createAlbumReleaseRequest(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ArtistErrorCode.ERR_FORBIDDEN_ARTIST_ACCESS.getMessage());

            verify(userRepository, times(1)).findById(userId);
            verify(artistRepository, times(1)).findById(artistId);
            verify(albumReleaseRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("비활성화된 아티스트면 앨범 등록 요청 생성에 실패한다")
        void createAlbumReleaseRequest_fail_whenArtistInactive() {
            // given
            Long userId = 1L;
            Long artistId = 10L;

            AlbumReleaseRequestCreateRequest request = new AlbumReleaseRequestCreateRequest(
                    artistId,
                    "Love poem",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    0
            );

            User user = mock(User.class);
            when(userRepository.findById(userId))
                    .thenReturn(java.util.Optional.of(user));

            Artist artist = Artist.create(
                    userId,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);
            artist.deactivate();

            when(artistRepository.findById(artistId))
                    .thenReturn(java.util.Optional.of(artist));

            // when & then
            assertThatThrownBy(() -> albumService.createAlbumReleaseRequest(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AlbumReleaseErrorCode.ERR_INACTIVE_ARTIST_CANNOT_REQUEST_ALBUM_RELEASE.getMessage());

            verify(userRepository, times(1)).findById(userId);
            verify(artistRepository, times(1)).findById(artistId);
            verify(albumReleaseRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("공개 예약 옵션이 범위를 벗어나면 앨범 등록 요청 생성에 실패한다")
        void createAlbumReleaseRequest_fail_whenInvalidPublishDelayDays() {
            // given
            Long userId = 1L;
            Long artistId = 10L;

            AlbumReleaseRequestCreateRequest request = new AlbumReleaseRequestCreateRequest(
                    artistId,
                    "Love poem",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    8
            );

            User user = mock(User.class);
            when(userRepository.findById(userId))
                    .thenReturn(java.util.Optional.of(user));

            Artist artist = Artist.create(
                    userId,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);

            when(artistRepository.findById(artistId))
                    .thenReturn(java.util.Optional.of(artist));

            // when & then
            assertThatThrownBy(() -> albumService.createAlbumReleaseRequest(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AlbumReleaseErrorCode.ERR_INVALID_PUBLISH_DELAY_DAYS.getMessage());

            verify(userRepository, times(1)).findById(userId);
            verify(artistRepository, times(1)).findById(artistId);
            verify(albumReleaseRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("동일한 진행 중 요청이 이미 있으면 앨범 등록 요청 생성에 실패한다")
        void createAlbumReleaseRequest_fail_whenAlreadyExists() {
            // given
            Long userId = 1L;
            Long artistId = 10L;

            AlbumReleaseRequestCreateRequest request = new AlbumReleaseRequestCreateRequest(
                    artistId,
                    "Love poem",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    0
            );

            User user = mock(User.class);
            when(userRepository.findById(userId))
                    .thenReturn(java.util.Optional.of(user));

            Artist artist = Artist.create(
                    userId,
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/artist.jpg"
            );
            ReflectionTestUtils.setField(artist, "id", artistId);

            when(artistRepository.findById(artistId))
                    .thenReturn(java.util.Optional.of(artist));

            when(albumReleaseRequestRepository.existsPendingRequest(
                    userId,
                    artistId,
                    request.title()
            )).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> albumService.createAlbumReleaseRequest(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AlbumReleaseErrorCode.ERR_ALBUM_RELEASE_ALREADY_EXISTS.getMessage());

            verify(userRepository, times(1)).findById(userId);
            verify(artistRepository, times(1)).findById(artistId);
            verify(albumReleaseRequestRepository, times(1))
                    .existsPendingRequest(userId, artistId, request.title());
            verify(albumReleaseRequestRepository, never()).save(any());
        }
    }
}