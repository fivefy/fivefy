package com.fivefy.domain.like.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.album.entity.Album;
import com.fivefy.domain.album.enums.AlbumErrorCode;
import com.fivefy.domain.album.repository.AlbumRepository;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.artist.repository.ArtistRepository;
import com.fivefy.domain.like.dto.response.LikeCreateResponse;
import com.fivefy.domain.like.dto.response.LikeGetResponse;
import com.fivefy.domain.like.entity.Like;
import com.fivefy.domain.like.enums.LikeErrorCode;
import com.fivefy.domain.like.enums.TargetType;
import com.fivefy.domain.like.repository.LikeRepository;
import com.fivefy.domain.notification.repository.NotificationOutboxRepository;
import com.fivefy.domain.track.entity.Track;
import com.fivefy.domain.track.enums.TrackErrorCode;
import com.fivefy.domain.track.repository.TrackRepository;
import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.enums.UserErrorCode;
import com.fivefy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TrackRepository trackRepository;

    @Mock
    private AlbumRepository albumRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private NotificationOutboxRepository outboxRepository;

    @InjectMocks
    private LikeService likeService;

    private User mockUser;
    private Track mockTrack;
    private Album mockAlbum;
    private Like mockLike;

    private static final Long USER_ID = 1L;
    private static final Long TRACK_ID = 2L;
    private static final Long ALBUM_ID = 3L;
    private static final Long LIKE_ID = 4L;
    private Artist mockArtist;

    @BeforeEach
    void setUp() {
        mockUser = mock(User.class);
        mockTrack = mock(Track.class);
        mockAlbum = mock(Album.class);
        mockArtist = mock(Artist.class);
        mockLike = Like.create(USER_ID, TRACK_ID, TargetType.TRACK);

        lenient().when(mockUser.getId()).thenReturn(USER_ID);
        lenient().when(mockUser.getName()).thenReturn("테스트유저");
        lenient().when(mockTrack.getId()).thenReturn(TRACK_ID);
        lenient().when(mockAlbum.getId()).thenReturn(ALBUM_ID);
    }

    @Nested
    @DisplayName("좋아요 등록")
    class createLike {

        @Test
        @DisplayName("좋아요 등록 성공 - TRACK")
        void createLike_track_success () {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));
            given(trackRepository.findById(TRACK_ID)).willReturn(Optional.of(mockTrack));
            given(mockTrack.getArtistId()).willReturn(1L);
            given(artistRepository.findById(1L)).willReturn(Optional.of(mockArtist));
            given(likeRepository.existsByUserIdAndTargetIdAndTargetType(
                    USER_ID, TRACK_ID, TargetType.TRACK)).willReturn(false);
            given(likeRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

            // when
            LikeCreateResponse response = likeService.createLike(TRACK_ID, TargetType.TRACK, USER_ID);

            // then
            assertThat(response.targetId()).isEqualTo(TRACK_ID);
            assertThat(response.targetType()).isEqualTo(TargetType.TRACK);
            verify(likeRepository).save(any(Like.class));
            verify(outboxRepository).save(any());
        }

        @Test
        @DisplayName("좋아요 등록 성공 - ALBUM")
        void createLike_album_success () {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));
            given(albumRepository.findById(ALBUM_ID)).willReturn(Optional.of(mockAlbum));
            given(mockAlbum.getArtistId()).willReturn(1L);
            given(artistRepository.findById(1L)).willReturn(Optional.of(mockArtist));
            given(likeRepository.existsByUserIdAndTargetIdAndTargetType(
                    USER_ID, ALBUM_ID, TargetType.ALBUM)).willReturn(false);
            given(likeRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

            // when
            LikeCreateResponse response = likeService.createLike(ALBUM_ID, TargetType.ALBUM, USER_ID);

            // then
            assertThat(response.targetId()).isEqualTo(ALBUM_ID);
            assertThat(response.targetType()).isEqualTo(TargetType.ALBUM);
            verify(likeRepository).save(any(Like.class));
            verify(outboxRepository).save(any());
        }

        // createLike 실패
        @Test
        @DisplayName("좋아요 등록 실패 - 존재하지 않는 유저")
        void createLike_userNotFound () {
            // given
            given(userRepository.findById(any())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> likeService.createLike(TRACK_ID, TargetType.TRACK, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(UserErrorCode.ERR_USER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("좋아요 등록 실패 - 존재하지 않는 트랙")
        void createLike_trackNotFound () {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));
            given(trackRepository.findById(any())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> likeService.createLike(TRACK_ID, TargetType.TRACK, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackErrorCode.ERR_TRACK_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("좋아요 등록 실패 - 존재하지 않는 앨범")
        void createLike_albumNotFound () {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));
            given(albumRepository.findById(any())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> likeService.createLike(ALBUM_ID, TargetType.ALBUM, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AlbumErrorCode.ERR_ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("좋아요 등록 실패 - 중복 (existsBy 검증)")
        void createLike_duplicateByExists () {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));
            given(trackRepository.findById(TRACK_ID)).willReturn(Optional.of(mockTrack));
            given(likeRepository.existsByUserIdAndTargetIdAndTargetType(USER_ID, TRACK_ID,
                    TargetType.TRACK)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> likeService.createLike(TRACK_ID, TargetType.TRACK, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(LikeErrorCode.ERR_LIKE_ALREADY_EXISTS.getMessage());

            verify(likeRepository, never()).save(any());
        }

        @Test
        @DisplayName("좋아요 등록 실패 - 중복 (DB UniqueConstraint 위반)")
        void createLike_duplicateByDBConstraint () {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));
            given(trackRepository.findById(TRACK_ID)).willReturn(Optional.of(mockTrack));
            given(likeRepository.existsByUserIdAndTargetIdAndTargetType(USER_ID, TRACK_ID,
                    TargetType.TRACK)).willReturn(false);
            given(likeRepository.save(any())).willThrow(DataIntegrityViolationException.class);

            // when & then
            assertThatThrownBy(() -> likeService.createLike(TRACK_ID, TargetType.TRACK, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(LikeErrorCode.ERR_LIKE_ALREADY_EXISTS.getMessage());
        }
    }

    @Nested
    @DisplayName("좋아요 목록 조회")
    class getLikes{

        @Test
        @DisplayName("좋아요 목록 조회 성공 - 전체 조회")
        void getLikes_success() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            LikeGetResponse likeGetResponse = new LikeGetResponse(LIKE_ID, TRACK_ID, TargetType.TRACK,
                    "title", "artist", LocalDateTime.now());
            Page<LikeGetResponse> page = new PageImpl<>(List.of(likeGetResponse), pageable, 1);

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));
            given(likeRepository.findLikesWithTarget(USER_ID, null, pageable)).willReturn(page);

            // when
            Page<LikeGetResponse> responses = likeService.getLikes(USER_ID, null, pageable);

            // then
            assertThat(responses.getContent()).hasSize(1);
            assertThat(responses.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("좋아요 목록 조회 성공 - targetType 필터링")
        void getLikes_withTargetTypeFilter() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            LikeGetResponse likeGetResponse = new LikeGetResponse(LIKE_ID, TRACK_ID, TargetType.TRACK,
                    "title", "artist", LocalDateTime.now());
            Page<LikeGetResponse> page = new PageImpl<>(List.of(likeGetResponse), pageable, 1);

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));
            given(likeRepository.findLikesWithTarget(USER_ID, TargetType.TRACK, pageable)).willReturn(page);

            // when
            Page<LikeGetResponse> responses = likeService.getLikes(USER_ID, TargetType.TRACK, pageable);

            // then
            assertThat(responses.getContent()).hasSize(1);
            assertThat(responses.getContent().get(0).targetType()).isEqualTo(TargetType.TRACK);
        }

        @Test
        @DisplayName("좋아요 목록 조회 실패 - 존재하지 않는 유저")
        void getLikes_userNotFound() {
            // given
            given(userRepository.findById(any())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> likeService.getLikes(USER_ID, null,
                    PageRequest.of(0, 20)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(UserErrorCode.ERR_USER_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("좋아요 취소")
    class deleteLike{

        @Test
        @DisplayName("좋아요 취소 성공")
        void deleteLike_success() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));
            given(likeRepository.findByIdAndUserId(LIKE_ID, USER_ID)).willReturn(Optional.of(mockLike));

            // when
            likeService.deleteLike(USER_ID, LIKE_ID);

            // then
            verify(likeRepository).delete(mockLike);
        }

        @Test
        @DisplayName("좋아요 취소 실패 - 유저 없음")
        void deleteLike_userNotFound() {
            // given
            given(userRepository.findById(any())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> likeService.deleteLike(USER_ID, LIKE_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(UserErrorCode.ERR_USER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("좋아요 취소 실패 - 좋아요 없음")
        void deleteLike_notFound() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));
            given(likeRepository.findByIdAndUserId(any(), any())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> likeService.deleteLike(USER_ID, LIKE_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(LikeErrorCode.ERR_LIKE_NOT_FOUND.getMessage());
        }
    }
}
