package com.fivefy.domain.playlisttrack.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.playlist.entity.Playlist;
import com.fivefy.domain.playlist.enums.PlaylistErrorCode;
import com.fivefy.domain.playlist.repository.PlaylistRepository;
import com.fivefy.domain.playlisttrack.dto.request.PlaylistTrackCreateRequest;
import com.fivefy.domain.playlisttrack.dto.request.PlaylistTrackOrderUpdateRequest;
import com.fivefy.domain.playlisttrack.dto.response.PlaylistTrackResponse;
import com.fivefy.domain.playlisttrack.entity.PlaylistTrack;
import com.fivefy.domain.playlisttrack.repository.PlaylistTrackRepository;
import com.fivefy.domain.track.repository.TrackRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaylistTrackServiceTest {

    @InjectMocks private
    PlaylistTrackService playlistTrackService;

    @Mock private PlaylistTrackRepository playlistTrackRepository;
    @Mock private PlaylistRepository playlistRepository;
    @Mock private TrackRepository trackRepository;

    @Nested
    @DisplayName("플레이리스트 트랙 추가")
    class AddTrack {

        @Test
        @DisplayName("플레이리스트 트랙 추가 성공")
        void addTrackSuccess() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            PlaylistTrackCreateRequest request = new PlaylistTrackCreateRequest(10L);

            Playlist playlist = Playlist.create(userId, "플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            PlaylistTrack savedTrack = PlaylistTrack.create(playlistId, 10L, 1);
            ReflectionTestUtils.setField(savedTrack, "id", 1L);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));
            given(trackRepository.existsById(10L))
                    .willReturn(true);
            given(playlistTrackRepository.existsByPlaylistIdAndTrackId(playlistId, 10L))
                    .willReturn(false);
            given(playlistTrackRepository.countByPlaylistId(playlistId))
                    .willReturn(0);
            given(playlistTrackRepository.saveAndFlush(any(PlaylistTrack.class)))
                    .willReturn(savedTrack);

            // when
            PlaylistTrackResponse result = playlistTrackService.addTrack(userId, playlistId, request);

            // then
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.playlistId()).isEqualTo(playlistId);
            assertThat(result.trackId()).isEqualTo(10L);
            assertThat(result.position()).isEqualTo(1);
        }

        @Test
        @DisplayName("존재하지 않는 트랙이면 예외 발생")
        void addTrackTrackNotFound() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            PlaylistTrackCreateRequest request = new PlaylistTrackCreateRequest(10L);

            Playlist playlist = Playlist.create(userId, "플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));
            given(trackRepository.existsById(10L))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> playlistTrackService.addTrack(userId, playlistId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.TRACK_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("플레이리스트가 없으면 예외 발생")
        void addTrackPlaylistNotFound() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            PlaylistTrackCreateRequest request = new PlaylistTrackCreateRequest(10L);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> playlistTrackService.addTrack(userId, playlistId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.PLAYLIST_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("본인 플레이리스트가 아니면 예외 발생")
        void addTrackForbidden() {
            // given
            Long userId = 1L;
            Long otherUserId = 2L;
            Long playlistId = 1L;
            PlaylistTrackCreateRequest request = new PlaylistTrackCreateRequest(10L);

            Playlist playlist = Playlist.create(otherUserId, "플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));

            // when & then
            assertThatThrownBy(() -> playlistTrackService.addTrack(userId, playlistId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.PLAYLIST_ACCESS_FORBIDDEN.getMessage());
        }

        @Test
        @DisplayName("이미 존재하는 트랙이면 예외 발생")
        void addTrackAlreadyExists() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            PlaylistTrackCreateRequest request = new PlaylistTrackCreateRequest(10L);

            Playlist playlist = Playlist.create(userId, "플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));
            given(trackRepository.existsById(10L))
                    .willReturn(true);
            given(playlistTrackRepository.existsByPlaylistIdAndTrackId(playlistId, 10L))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> playlistTrackService.addTrack(userId, playlistId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.PLAYLIST_TRACK_ALREADY_EXISTS.getMessage());

            verify(playlistTrackRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("트랙 추가 중 playlist-track 유니크 충돌 시 예외 발생")
        void addTrackConstraintConflict_trackAlreadyExists() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            PlaylistTrackCreateRequest request = new PlaylistTrackCreateRequest(10L);

            Playlist playlist = Playlist.create(userId, "플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));
            given(trackRepository.existsById(10L))
                    .willReturn(true);
            given(playlistTrackRepository.existsByPlaylistIdAndTrackId(playlistId, 10L))
                    .willReturn(false);
            given(playlistTrackRepository.countByPlaylistId(playlistId))
                    .willReturn(0);
            given(playlistTrackRepository.saveAndFlush(any(PlaylistTrack.class)))
                    .willThrow(constraintViolationException("uk_playlist_track_playlist_track"));

            // when & then
            assertThatThrownBy(() -> playlistTrackService.addTrack(userId, playlistId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.PLAYLIST_TRACK_ALREADY_EXISTS.getMessage());
        }

        @Test
        @DisplayName("트랙 추가 중 position 유니크 충돌 시 예외 발생")
        void addTrackConstraintConflict_positionConflict() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            PlaylistTrackCreateRequest request = new PlaylistTrackCreateRequest(10L);

            Playlist playlist = Playlist.create(userId, "플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));
            given(trackRepository.existsById(10L))
                    .willReturn(true);
            given(playlistTrackRepository.existsByPlaylistIdAndTrackId(playlistId, 10L))
                    .willReturn(false);
            given(playlistTrackRepository.countByPlaylistId(playlistId))
                    .willReturn(0);
            given(playlistTrackRepository.saveAndFlush(any(PlaylistTrack.class)))
                    .willThrow(constraintViolationException("uk_playlist_track_playlist_position"));

            // when & then
            assertThatThrownBy(() -> playlistTrackService.addTrack(userId, playlistId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.PLAYLIST_TRACK_POSITION_CONFLICT.getMessage());
        }

        @Test
        @DisplayName("알 수 없는 constraint면 원본 예외를 그대로 던진다")
        void addTrackUnknownConstraint() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            PlaylistTrackCreateRequest request = new PlaylistTrackCreateRequest(10L);

            Playlist playlist = Playlist.create(userId, "플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));
            given(trackRepository.existsById(10L))
                    .willReturn(true);
            given(playlistTrackRepository.existsByPlaylistIdAndTrackId(playlistId, 10L))
                    .willReturn(false);
            given(playlistTrackRepository.countByPlaylistId(playlistId))
                    .willReturn(0);
            given(playlistTrackRepository.saveAndFlush(any(PlaylistTrack.class)))
                    .willThrow(constraintViolationException("unknown_constraint"));

            // when & then
            assertThatThrownBy(() -> playlistTrackService.addTrack(userId, playlistId, request))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("constraint 이름을 추출할 수 없으면 원본 예외를 그대로 던진다")
        void addTrackConstraintNameNotExtracted() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            PlaylistTrackCreateRequest request = new PlaylistTrackCreateRequest(10L);

            Playlist playlist = Playlist.create(userId, "플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));
            given(trackRepository.existsById(10L))
                    .willReturn(true);
            given(playlistTrackRepository.existsByPlaylistIdAndTrackId(playlistId, 10L))
                    .willReturn(false);
            given(playlistTrackRepository.countByPlaylistId(playlistId))
                    .willReturn(0);
            given(playlistTrackRepository.saveAndFlush(any(PlaylistTrack.class)))
                    .willThrow(new DataIntegrityViolationException("data integrity violation"));

            // when & then
            assertThatThrownBy(() -> playlistTrackService.addTrack(userId, playlistId, request))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("플레이리스트 트랙 목록 조회")
    class GetTracks {

        @Test
        @DisplayName("플레이리스트 트랙 목록 조회 성공")
        void getTracksSuccess() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;

            Playlist playlist = Playlist.create(userId, "플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            PlaylistTrack track1 = PlaylistTrack.create(playlistId, 10L, 1);
            ReflectionTestUtils.setField(track1, "id", 1L);

            PlaylistTrack track2 = PlaylistTrack.create(playlistId, 20L, 2);
            ReflectionTestUtils.setField(track2, "id", 2L);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));
            given(playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(playlistId))
                    .willReturn(List.of(track1, track2));

            // when
            List<PlaylistTrackResponse> result = playlistTrackService.getTracks(userId, playlistId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).trackId()).isEqualTo(10L);
            assertThat(result.get(0).position()).isEqualTo(1);
            assertThat(result.get(1).trackId()).isEqualTo(20L);
            assertThat(result.get(1).position()).isEqualTo(2);
        }

        @Test
        @DisplayName("플레이리스트가 없으면 예외 발생")
        void getTracksPlaylistNotFound() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> playlistTrackService.getTracks(userId, playlistId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.PLAYLIST_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("본인 플레이리스트가 아니면 예외 발생")
        void getTracksForbidden() {
            // given
            Long userId = 1L;
            Long otherUserId = 2L;
            Long playlistId = 1L;

            Playlist playlist = Playlist.create(otherUserId, "플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));

            // when & then
            assertThatThrownBy(() -> playlistTrackService.getTracks(userId, playlistId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.PLAYLIST_ACCESS_FORBIDDEN.getMessage());
        }
    }

    @Nested
    @DisplayName("플레이리스트 트랙 순서 변경")
    class UpdateTrackOrder {

        @Test
        @DisplayName("트랙 순서 변경 성공")
        void updateTrackOrderSuccess() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            PlaylistTrackOrderUpdateRequest request = new PlaylistTrackOrderUpdateRequest(30L, 1);

            Playlist playlist = Playlist.create(userId, "플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            PlaylistTrack track1 = PlaylistTrack.create(playlistId, 10L, 1);
            PlaylistTrack track2 = PlaylistTrack.create(playlistId, 20L, 2);
            PlaylistTrack track3 = PlaylistTrack.create(playlistId, 30L, 3);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));
            given(playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(playlistId))
                    .willReturn(new ArrayList<>(List.of(track1, track2, track3)));
            given(playlistTrackRepository.findByPlaylistIdAndPositionBetweenOrderByPositionAsc(playlistId, 1, 2))
                    .willReturn(List.of(track1, track2));

            // when
            playlistTrackService.updateTrackOrder(userId, playlistId, request);

            // then
            assertThat(track3.getPosition()).isEqualTo(1);
            assertThat(track1.getPosition()).isEqualTo(2);
            assertThat(track2.getPosition()).isEqualTo(3);

            verify(playlistTrackRepository, times(2)).flush();
        }

        @Test
        @DisplayName("플레이리스트가 없으면 예외 발생")
        void updateTrackOrderPlaylistNotFound() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            PlaylistTrackOrderUpdateRequest request = new PlaylistTrackOrderUpdateRequest(10L, 1);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> playlistTrackService.updateTrackOrder(userId, playlistId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.PLAYLIST_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("본인 플레이리스트가 아니면 예외 발생")
        void updateTrackOrderForbidden() {
            // given
            Long userId = 1L;
            Long otherUserId = 2L;
            Long playlistId = 1L;
            PlaylistTrackOrderUpdateRequest request = new PlaylistTrackOrderUpdateRequest(10L, 1);

            Playlist playlist = Playlist.create(otherUserId, "플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));

            // when & then
            assertThatThrownBy(() -> playlistTrackService.updateTrackOrder(userId, playlistId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.PLAYLIST_UPDATE_FORBIDDEN.getMessage());
        }

        @Test
        @DisplayName("대상 트랙이 없으면 예외 발생")
        void updateTrackOrderTrackNotFound() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            PlaylistTrackOrderUpdateRequest request = new PlaylistTrackOrderUpdateRequest(99L, 1);

            Playlist playlist = Playlist.create(userId, "플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            PlaylistTrack track1 = PlaylistTrack.create(playlistId, 10L, 1);
            PlaylistTrack track2 = PlaylistTrack.create(playlistId, 20L, 2);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));
            given(playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(playlistId))
                    .willReturn(List.of(track1, track2));

            // when & then
            assertThatThrownBy(() -> playlistTrackService.updateTrackOrder(userId, playlistId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.PLAYLIST_TRACK_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("이동 위치가 범위를 벗어나면 예외 발생")
        void updateTrackOrderInvalidPosition() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            PlaylistTrackOrderUpdateRequest request = new PlaylistTrackOrderUpdateRequest(10L, 5);

            Playlist playlist = Playlist.create(userId, "플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            PlaylistTrack track1 = PlaylistTrack.create(playlistId, 10L, 1);
            PlaylistTrack track2 = PlaylistTrack.create(playlistId, 20L, 2);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));
            given(playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(playlistId))
                    .willReturn(List.of(track1, track2));

            // when & then
            assertThatThrownBy(() -> playlistTrackService.updateTrackOrder(userId, playlistId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.INVALID_PLAYLIST_TRACK_POSITION.getMessage());
        }

        @Test
        @DisplayName("현재 위치와 같으면 flush 없이 종료")
        void updateTrackOrderSamePosition() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            PlaylistTrackOrderUpdateRequest request = new PlaylistTrackOrderUpdateRequest(10L, 1);

            Playlist playlist = Playlist.create(userId, "플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            PlaylistTrack track1 = PlaylistTrack.create(playlistId, 10L, 1);
            PlaylistTrack track2 = PlaylistTrack.create(playlistId, 20L, 2);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));
            given(playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(playlistId))
                    .willReturn(List.of(track1, track2));

            // when
            playlistTrackService.updateTrackOrder(userId, playlistId, request);

            // then
            verify(playlistTrackRepository, never()).flush();
            assertThat(track1.getPosition()).isEqualTo(1);
            assertThat(track2.getPosition()).isEqualTo(2);
        }

        @Test
        @DisplayName("position이 1 미만이면 예외 발생")
        void updateTrackOrderLessThanOne() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            PlaylistTrackOrderUpdateRequest request =
                    new PlaylistTrackOrderUpdateRequest(10L, 0);

            Playlist playlist = Playlist.create(userId, "플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            PlaylistTrack track1 = PlaylistTrack.create(playlistId, 10L, 1);
            PlaylistTrack track2 = PlaylistTrack.create(playlistId, 20L, 2);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));
            given(playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(playlistId))
                    .willReturn(List.of(track1, track2));

            // when & then
            assertThatThrownBy(() ->
                    playlistTrackService.updateTrackOrder(userId, playlistId, request)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.INVALID_PLAYLIST_TRACK_POSITION.getMessage());
        }

        @Test
        @DisplayName("트랙 순서 변경 중 position 충돌 시 예외 발생")
        void updateTrackOrderPositionConflict() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            PlaylistTrackOrderUpdateRequest request = new PlaylistTrackOrderUpdateRequest(30L, 1);

            Playlist playlist = Playlist.create(userId, "플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            PlaylistTrack track1 = PlaylistTrack.create(playlistId, 10L, 1);
            PlaylistTrack track2 = PlaylistTrack.create(playlistId, 20L, 2);
            PlaylistTrack track3 = PlaylistTrack.create(playlistId, 30L, 3);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));
            given(playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(playlistId))
                    .willReturn(new ArrayList<>(List.of(track1, track2, track3)));

            doThrow(constraintViolationException("uk_playlist_track_playlist_position"))
                    .when(playlistTrackRepository).flush();

            // when & then
            assertThatThrownBy(() -> playlistTrackService.updateTrackOrder(userId, playlistId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.PLAYLIST_TRACK_POSITION_CONFLICT.getMessage());
        }

        @Test
        @DisplayName("트랙 순서를 아래로 이동하면 영향 범위만 앞으로 당겨진다")
        void updateTrackOrderMoveDownSuccess() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            PlaylistTrackOrderUpdateRequest request = new PlaylistTrackOrderUpdateRequest(10L, 3);

            Playlist playlist = Playlist.create(userId, "플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            PlaylistTrack track1 = PlaylistTrack.create(playlistId, 10L, 1);
            PlaylistTrack track2 = PlaylistTrack.create(playlistId, 20L, 2);
            PlaylistTrack track3 = PlaylistTrack.create(playlistId, 30L, 3);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));
            given(playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(playlistId))
                    .willReturn(new ArrayList<>(List.of(track1, track2, track3)));

            // 1 -> 3 이동이므로 영향 범위는 2 ~ 3
            given(playlistTrackRepository.findByPlaylistIdAndPositionBetweenOrderByPositionAsc(playlistId, 2, 3))
                    .willReturn(List.of(track2, track3));

            // when
            playlistTrackService.updateTrackOrder(userId, playlistId, request);

            // then
            assertThat(track1.getPosition()).isEqualTo(3);
            assertThat(track2.getPosition()).isEqualTo(1);
            assertThat(track3.getPosition()).isEqualTo(2);

            verify(playlistTrackRepository, times(2)).flush();
        }

        @Test
        @DisplayName("트랙 순서 아래 이동 중 position 충돌 시 예외 발생")
        void updateTrackOrderMoveDownPositionConflict() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            PlaylistTrackOrderUpdateRequest request = new PlaylistTrackOrderUpdateRequest(10L, 3);

            Playlist playlist = Playlist.create(userId, "플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            PlaylistTrack track1 = PlaylistTrack.create(playlistId, 10L, 1);
            PlaylistTrack track2 = PlaylistTrack.create(playlistId, 20L, 2);
            PlaylistTrack track3 = PlaylistTrack.create(playlistId, 30L, 3);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));
            given(playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(playlistId))
                    .willReturn(new ArrayList<>(List.of(track1, track2, track3)));
            given(playlistTrackRepository.findByPlaylistIdAndPositionBetweenOrderByPositionAsc(playlistId, 2, 3))
                    .willReturn(List.of(track2, track3));

            doThrow(constraintViolationException("uk_playlist_track_playlist_position"))
                    .when(playlistTrackRepository).flush();

            // when & then
            assertThatThrownBy(() -> playlistTrackService.updateTrackOrder(userId, playlistId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.PLAYLIST_TRACK_POSITION_CONFLICT.getMessage());
        }
    }

    @Nested
    @DisplayName("플레이리스트 트랙 삭제")
    class DeleteTrack {

        @Test
        @DisplayName("플레이리스트 트랙 삭제 성공")
        void deleteTrackSuccess() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            Long trackId = 20L;

            Playlist playlist = Playlist.create(userId, "플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            PlaylistTrack track2 = PlaylistTrack.create(playlistId, 20L, 2);
            PlaylistTrack track3 = PlaylistTrack.create(playlistId, 30L, 3);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));
            given(playlistTrackRepository.findByPlaylistIdAndTrackId(playlistId, trackId))
                    .willReturn(Optional.of(track2));
            given(playlistTrackRepository.findByPlaylistIdAndPositionGreaterThanOrderByPositionAsc(playlistId, 2))
                    .willReturn(List.of(track3));

            // when
            playlistTrackService.deleteTrack(userId, playlistId, trackId);

            // then
            verify(playlistTrackRepository).delete(track2);
            verify(playlistTrackRepository, times(2)).flush();
            assertThat(track3.getPosition()).isEqualTo(2);
        }

        @Test
        @DisplayName("플레이리스트가 없으면 예외 발생")
        void deleteTrackPlaylistNotFound() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            Long trackId = 10L;

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> playlistTrackService.deleteTrack(userId, playlistId, trackId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.PLAYLIST_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("본인 플레이리스트가 아니면 예외 발생")
        void deleteTrackForbidden() {
            // given
            Long userId = 1L;
            Long otherUserId = 2L;
            Long playlistId = 1L;
            Long trackId = 10L;

            Playlist playlist = Playlist.create(otherUserId, "플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));

            // when & then
            assertThatThrownBy(() -> playlistTrackService.deleteTrack(userId, playlistId, trackId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.PLAYLIST_DELETE_FORBIDDEN.getMessage());
        }

        @Test
        @DisplayName("삭제 대상 트랙이 없으면 예외 발생")
        void deleteTrackTrackNotFound() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            Long trackId = 99L;

            Playlist playlist = Playlist.create(userId, "플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));
            given(playlistTrackRepository.findByPlaylistIdAndTrackId(playlistId, trackId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> playlistTrackService.deleteTrack(userId, playlistId, trackId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.PLAYLIST_TRACK_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("트랙 삭제 중 position 충돌 시 예외 발생")
        void deleteTrackPositionConflict() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            Long trackId = 20L;

            Playlist playlist = Playlist.create(userId, "플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            PlaylistTrack track2 = PlaylistTrack.create(playlistId, 20L, 2);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));
            given(playlistTrackRepository.findByPlaylistIdAndTrackId(playlistId, trackId))
                    .willReturn(Optional.of(track2));

            doThrow(constraintViolationException("uk_playlist_track_playlist_position"))
                    .when(playlistTrackRepository).flush();

            // when & then
            assertThatThrownBy(() -> playlistTrackService.deleteTrack(userId, playlistId, trackId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.PLAYLIST_TRACK_POSITION_CONFLICT.getMessage());
        }
    }

    private DataIntegrityViolationException constraintViolationException(String constraintName) {
        ConstraintViolationException cause =
                new ConstraintViolationException("unique constraint violation", new SQLException(), constraintName);

        return new DataIntegrityViolationException("data integrity violation", cause);
    }
}
