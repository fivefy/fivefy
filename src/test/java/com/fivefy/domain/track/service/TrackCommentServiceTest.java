package com.fivefy.domain.track.service;

import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.album.entity.Album;
import com.fivefy.domain.album.enums.AlbumStatus;
import com.fivefy.domain.album.repository.AlbumRepository;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.artist.enums.ArtistType;
import com.fivefy.domain.artist.repository.ArtistRepository;
import com.fivefy.domain.track.dto.request.TrackCommentCreateRequest;
import com.fivefy.domain.track.dto.response.TrackCommentResponse;
import com.fivefy.domain.track.entity.Track;
import com.fivefy.domain.track.entity.TrackComment;
import com.fivefy.domain.track.enums.TrackCommentErrorCode;
import com.fivefy.domain.track.enums.TrackErrorCode;
import com.fivefy.domain.track.enums.TrackStatus;
import com.fivefy.domain.track.enums.TrackType;
import com.fivefy.domain.track.repository.TrackCommentRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TrackCommentService의 비즈니스 로직을 검증하는 단위 테스트
 *
 * 트랙 댓글 작성 기능 검증
 * 트랙 댓글 목록 조회 기능 검증
 */
@ExtendWith(MockitoExtension.class)
class TrackCommentServiceTest {

    @Mock
    private TrackCommentRepository trackCommentRepository;

    @Mock
    private TrackRepository trackRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AlbumRepository albumRepository;

    @Mock
    private ArtistRepository artistRepository;

    @InjectMocks
    private TrackCommentService trackCommentService;

    @Nested
    @DisplayName("트랙 댓글 작성")
    class CreateTrackComment {

        @Test
        @DisplayName("트랙 댓글 작성 성공")
        void createTrackComment_success() {
            Long userId = 1L;
            Long trackId = 1L;

            TrackCommentCreateRequest request =
                    new TrackCommentCreateRequest("댓글입니다");

            User user = mock(User.class);
            when(userRepository.findByIdAndDeletedAtIsNull(userId))
                    .thenReturn(Optional.of(user));

            // 공개된 자유 창작 트랙 조회 mock 구성
            stubPublishedFreeCreationTrack(trackId);

            when(trackCommentRepository.save(any(TrackComment.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            TrackCommentResponse response =
                    trackCommentService.createTrackComment(userId, trackId, request);

            assertThat(response.content()).isEqualTo("댓글입니다");
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.trackId()).isEqualTo(trackId);
        }

        @Test
        @DisplayName("정식 발매 트랙 댓글 작성 성공")
        void createTrackComment_success_whenOfficialRelease() {
            Long userId = 1L;
            Long trackId = 1L;
            Long artistId = 10L;
            Long albumId = 100L;

            TrackCommentCreateRequest request =
                    new TrackCommentCreateRequest("댓글입니다");

            User user = mock(User.class);
            when(userRepository.findByIdAndDeletedAtIsNull(userId))
                    .thenReturn(Optional.of(user));

            // 공개된 정식 발매 트랙 조회 mock 구성
            stubPublishedOfficialReleaseTrack(trackId, artistId, albumId);
            // 공개된 앨범 조회 mock 구성
            stubPublishedAlbum(albumId, artistId);
            // 삭제되지 않은 아티스트 조회 mock 구성
            stubNotDeletedArtist(artistId);

            when(trackCommentRepository.save(any(TrackComment.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            TrackCommentResponse response =
                    trackCommentService.createTrackComment(userId, trackId, request);

            assertThat(response.content()).isEqualTo("댓글입니다");
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.trackId()).isEqualTo(trackId);
        }

        @Test
        @DisplayName("존재하지 않는 유저면 트랙 댓글 작성 실패")
        void createTrackComment_fail_whenUserNotFound() {
            Long userId = 1L;
            Long trackId = 1L;

            TrackCommentCreateRequest request =
                    new TrackCommentCreateRequest("댓글입니다");

            when(userRepository.findByIdAndDeletedAtIsNull(userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    trackCommentService.createTrackComment(userId, trackId, request)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(UserErrorCode.ERR_USER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("존재하지 않는 트랙이면 트랙 댓글 작성 실패")
        void createTrackComment_fail_whenTrackNotFound() {
            Long userId = 1L;
            Long trackId = 1L;

            TrackCommentCreateRequest request =
                    new TrackCommentCreateRequest("댓글입니다");

            User user = mock(User.class);
            when(userRepository.findByIdAndDeletedAtIsNull(userId))
                    .thenReturn(Optional.of(user));

            when(trackRepository.findById(trackId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    trackCommentService.createTrackComment(userId, trackId, request)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackErrorCode.ERR_TRACK_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("비공개 트랙이면 트랙 댓글 작성 실패")
        void createTrackComment_fail_whenTrackUnpublished() {
            Long userId = 1L;
            Long trackId = 1L;

            TrackCommentCreateRequest request =
                    new TrackCommentCreateRequest("댓글입니다");

            User user = mock(User.class);
            when(userRepository.findByIdAndDeletedAtIsNull(userId))
                    .thenReturn(Optional.of(user));

            Track track = mock(Track.class);
            when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));
            when(track.getDeletedAt()).thenReturn(null);
            when(track.getStatus()).thenReturn(TrackStatus.UNPUBLISHED);

            assertThatThrownBy(() ->
                    trackCommentService.createTrackComment(userId, trackId, request)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackErrorCode.ERR_TRACK_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("트랙 댓글 목록 조회")
    class GetTrackComments {

        @Test
        @DisplayName("FREE_CREATION 트랙 댓글 목록 조회 성공")
        void getTrackComments_success_whenFreeCreation() {
            Long trackId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            // 공개된 자유 창작 트랙 조회 mock 구성
            stubPublishedFreeCreationTrack(trackId);

            TrackComment comment = mock(TrackComment.class);
            when(comment.getId()).thenReturn(1L);
            when(comment.getUserId()).thenReturn(10L);
            when(comment.getTrackId()).thenReturn(trackId);
            when(comment.getContent()).thenReturn("댓글");
            when(comment.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 4, 21, 12, 0, 0));
            when(comment.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 4, 21, 12, 0, 0));

            Page<TrackComment> page = new PageImpl<>(List.of(comment), pageable, 1);

            when(trackCommentRepository.getTrackComments(trackId, pageable))
                    .thenReturn(page);

            PageResponse<TrackCommentResponse> response =
                    trackCommentService.getTrackComments(trackId, pageable);

            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).commentId()).isEqualTo(1L);
            assertThat(response.content().get(0).userId()).isEqualTo(10L);
            assertThat(response.content().get(0).trackId()).isEqualTo(trackId);
            assertThat(response.content().get(0).content()).isEqualTo("댓글");
            assertThat(response.page()).isEqualTo(0);
            assertThat(response.size()).isEqualTo(20);
            assertThat(response.totalElements()).isEqualTo(1);
            assertThat(response.totalPages()).isEqualTo(1);
        }

        @Test
        @DisplayName("OFFICIAL_RELEASE 트랙 댓글 목록 조회 성공")
        void getTrackComments_success_whenOfficialRelease() {
            Long trackId = 1L;
            Long artistId = 10L;
            Long albumId = 100L;
            Pageable pageable = PageRequest.of(0, 20);

            // 공개된 정식 발매 트랙 조회 mock 구성
            stubPublishedOfficialReleaseTrack(trackId, artistId, albumId);
            // 공개된 앨범 조회 mock 구성
            stubPublishedAlbum(albumId, artistId);
            // 삭제되지 않은 아티스트 조회 mock 구성
            stubNotDeletedArtist(artistId);

            when(trackCommentRepository.getTrackComments(trackId, pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            PageResponse<TrackCommentResponse> response =
                    trackCommentService.getTrackComments(trackId, pageable);

            assertThat(response.content()).isEmpty();
            assertThat(response.page()).isEqualTo(0);
            assertThat(response.size()).isEqualTo(20);
            assertThat(response.totalElements()).isZero();
            assertThat(response.totalPages()).isZero();
        }

        @Test
        @DisplayName("존재하지 않는 트랙이면 트랙 댓글 목록 조회 실패")
        void getTrackComments_fail_whenTrackNotFound() {
            Long trackId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            when(trackRepository.findById(trackId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    trackCommentService.getTrackComments(trackId, pageable)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackErrorCode.ERR_TRACK_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("비공개 트랙이면 트랙 댓글 목록 조회 실패")
        void getTrackComments_fail_whenTrackUnpublished() {
            Long trackId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            Track track = mock(Track.class);
            when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));
            when(track.getDeletedAt()).thenReturn(null);
            when(track.getStatus()).thenReturn(TrackStatus.UNPUBLISHED);

            assertThatThrownBy(() ->
                    trackCommentService.getTrackComments(trackId, pageable)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackErrorCode.ERR_TRACK_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("정식 발매 트랙인데 미공개 앨범이면 트랙 댓글 목록 조회 실패")
        void getTrackComments_fail_whenOfficialReleaseAlbumUnpublished() {
            Long trackId = 1L;
            Long artistId = 10L;
            Long albumId = 100L;
            Pageable pageable = PageRequest.of(0, 20);

            Track track = mock(Track.class);
            when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));
            when(track.getDeletedAt()).thenReturn(null);
            when(track.getStatus()).thenReturn(TrackStatus.PUBLISHED);
            when(track.getTrackType()).thenReturn(TrackType.OFFICIAL_RELEASE);
            when(track.getAlbumId()).thenReturn(albumId);

            Album album = Album.create(
                    artistId,
                    "Palette",
                    "정규 앨범",
                    "https://example.com/cover.jpg",
                    null
            );
            ReflectionTestUtils.setField(album, "id", albumId);
            ReflectionTestUtils.setField(album, "status", AlbumStatus.UNPUBLISHED);

            when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));

            assertThatThrownBy(() ->
                    trackCommentService.getTrackComments(trackId, pageable)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackErrorCode.ERR_TRACK_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("트랙 댓글 수정")
    class UpdateTrackComment {

        @Test
        @DisplayName("트랙 댓글 수정 성공")
        void updateTrackComment_success() {
            Long userId = 1L;
            Long trackId = 1L;
            Long commentId = 10L;

            TrackCommentCreateRequest request =
                    new TrackCommentCreateRequest("수정된 댓글");

            // 공개된 자유 창작 트랙 조회 mock 구성
            stubPublishedFreeCreationTrack(trackId);

            TrackComment comment = mock(TrackComment.class);
            when(comment.getUserId()).thenReturn(userId);
            when(comment.getTrackId()).thenReturn(trackId);
            when(comment.getDeletedAt()).thenReturn(null);

            when(trackCommentRepository.findById(commentId))
                    .thenReturn(Optional.of(comment));

            // 수정 동작 mock
            when(comment.getContent()).thenReturn("수정된 댓글");

            TrackCommentResponse response =
                    trackCommentService.updateTrackComment(userId, trackId, commentId, request);

            assertThat(response.content()).isEqualTo("수정된 댓글");
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.trackId()).isEqualTo(trackId);
        }

        @Test
        @DisplayName("존재하지 않는 댓글이면 수정 실패")
        void updateTrackComment_fail_whenCommentNotFound() {
            Long userId = 1L;
            Long trackId = 1L;
            Long commentId = 10L;

            TrackCommentCreateRequest request =
                    new TrackCommentCreateRequest("수정된 댓글");

            stubPublishedFreeCreationTrack(trackId);

            when(trackCommentRepository.findById(commentId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    trackCommentService.updateTrackComment(userId, trackId, commentId, request)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackCommentErrorCode.ERR_TRACK_COMMENT_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("삭제된 댓글이면 수정 실패")
        void updateTrackComment_fail_whenDeletedComment() {
            Long userId = 1L;
            Long trackId = 1L;
            Long commentId = 10L;

            TrackCommentCreateRequest request =
                    new TrackCommentCreateRequest("수정된 댓글");

            stubPublishedFreeCreationTrack(trackId);

            TrackComment comment = mock(TrackComment.class);
            when(comment.getDeletedAt()).thenReturn(LocalDateTime.now());

            when(trackCommentRepository.findById(commentId))
                    .thenReturn(Optional.of(comment));

            assertThatThrownBy(() ->
                    trackCommentService.updateTrackComment(userId, trackId, commentId, request)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackCommentErrorCode.ERR_DELETED_TRACK_COMMENT_CANNOT_BE_UPDATED.getMessage());
        }

        @Test
        @DisplayName("작성자가 아니면 수정 실패")
        void updateTrackComment_fail_whenNotOwner() {
            Long userId = 1L;
            Long trackId = 1L;
            Long commentId = 10L;

            TrackCommentCreateRequest request =
                    new TrackCommentCreateRequest("수정된 댓글");

            stubPublishedFreeCreationTrack(trackId);

            TrackComment comment = mock(TrackComment.class);
            when(comment.getUserId()).thenReturn(999L); // 다른 사용자
            when(comment.getDeletedAt()).thenReturn(null);

            when(trackCommentRepository.findById(commentId))
                    .thenReturn(Optional.of(comment));

            assertThatThrownBy(() ->
                    trackCommentService.updateTrackComment(userId, trackId, commentId, request)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackCommentErrorCode.ERR_FORBIDDEN_TRACK_COMMENT_UPDATE.getMessage());
        }

        @Test
        @DisplayName("비공개 트랙이면 수정 실패")
        void updateTrackComment_fail_whenTrackUnpublished() {
            Long userId = 1L;
            Long trackId = 1L;
            Long commentId = 10L;

            TrackCommentCreateRequest request =
                    new TrackCommentCreateRequest("수정된 댓글");

            Track track = mock(Track.class);
            when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));
            when(track.getDeletedAt()).thenReturn(null);
            when(track.getStatus()).thenReturn(TrackStatus.UNPUBLISHED);

            assertThatThrownBy(() ->
                    trackCommentService.updateTrackComment(userId, trackId, commentId, request)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackErrorCode.ERR_TRACK_NOT_FOUND.getMessage());
        }
    }

    // 공개된 자유 창작 트랙 조회 mock 구성
    private Track stubPublishedFreeCreationTrack(Long trackId) {
        Track track = mock(Track.class);
        when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));
        when(track.getDeletedAt()).thenReturn(null);
        when(track.getStatus()).thenReturn(TrackStatus.PUBLISHED);
        when(track.getTrackType()).thenReturn(TrackType.FREE_CREATION);
        return track;
    }

    // 공개된 정식 발매 트랙 조회 mock 구성
    private Track stubPublishedOfficialReleaseTrack(Long trackId, Long artistId, Long albumId) {
        Track track = mock(Track.class);
        when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));
        when(track.getDeletedAt()).thenReturn(null);
        when(track.getStatus()).thenReturn(TrackStatus.PUBLISHED);
        when(track.getTrackType()).thenReturn(TrackType.OFFICIAL_RELEASE);
        when(track.getArtistId()).thenReturn(artistId);
        when(track.getAlbumId()).thenReturn(albumId);
        return track;
    }

    // 공개된 앨범 조회 mock 구성
    private void stubPublishedAlbum(Long albumId, Long artistId) {
        Album album = Album.create(
                artistId,
                "Palette",
                "정규 앨범",
                "https://example.com/cover.jpg",
                null
        );
        ReflectionTestUtils.setField(album, "id", albumId);
        album.publish();

        when(albumRepository.findById(albumId)).thenReturn(Optional.of(album));
    }

    // 삭제되지 않은 아티스트 조회 mock 구성
    private void stubNotDeletedArtist(Long artistId) {
        Artist artist = Artist.create(
                1L,
                "아이유",
                ArtistType.SOLO,
                "가수",
                "https://example.com/artist.jpg"
        );
        ReflectionTestUtils.setField(artist, "id", artistId);

        when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));
    }


}