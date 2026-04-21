package com.fivefy.domain.track.service;

import com.fivefy.domain.album.repository.AlbumRepository;
import com.fivefy.domain.artist.repository.ArtistRepository;
import com.fivefy.domain.track.dto.request.TrackCommentCreateRequest;
import com.fivefy.domain.track.dto.response.TrackCommentResponse;
import com.fivefy.domain.track.entity.Track;
import com.fivefy.domain.track.enums.TrackErrorCode;
import com.fivefy.domain.track.enums.TrackStatus;
import com.fivefy.domain.track.enums.TrackType;
import com.fivefy.domain.track.repository.TrackCommentRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackCommentServiceTest {

    @InjectMocks
    private TrackCommentService trackCommentService;

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

    private User user;
    private Track track;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        track = mock(Track.class);
    }

    @Nested
    @DisplayName("트랙 댓글 작성")
    class CreateTrackComment {

        @Test
        @DisplayName("트랙 댓글 작성 성공")
        void createTrackComment_success() {
            Long userId = 1L;
            Long trackId = 1L;
            TrackCommentCreateRequest request = new TrackCommentCreateRequest("댓글입니다");

            when(userRepository.findByIdAndDeletedAtIsNull(userId))
                    .thenReturn(Optional.of(user));

            when(trackRepository.findById(trackId))
                    .thenReturn(Optional.of(track));

            when(track.getDeletedAt()).thenReturn(null);
            when(track.getStatus()).thenReturn(TrackStatus.PUBLISHED);
            when(track.getTrackType()).thenReturn(TrackType.FREE_CREATION);

            when(trackCommentRepository.save(any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            TrackCommentResponse response =
                    trackCommentService.createTrackComment(userId, trackId, request);

            assertThat(response).isNotNull();
            assertThat(response.content()).isEqualTo("댓글입니다");
        }

        @Test
        @DisplayName("존재하지 않는 유저면 트랙 댓글 작성 실패")
        void createTrackComment_fail_userNotFound() {
            Long userId = 1L;
            Long trackId = 1L;
            TrackCommentCreateRequest request = new TrackCommentCreateRequest("댓글입니다");

            when(userRepository.findByIdAndDeletedAtIsNull(userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    trackCommentService.createTrackComment(userId, trackId, request)
            ).hasMessage(UserErrorCode.ERR_USER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("존재하지 않는 트랙이면 트랙 댓글 작성 실패")
        void createTrackComment_fail_trackNotFound() {
            Long userId = 1L;
            Long trackId = 1L;
            TrackCommentCreateRequest request = new TrackCommentCreateRequest("댓글입니다");

            when(userRepository.findByIdAndDeletedAtIsNull(userId))
                    .thenReturn(Optional.of(user));

            when(trackRepository.findById(trackId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    trackCommentService.createTrackComment(userId, trackId, request)
            ).hasMessage(TrackErrorCode.ERR_TRACK_NOT_FOUND.getMessage());
        }
    }
}