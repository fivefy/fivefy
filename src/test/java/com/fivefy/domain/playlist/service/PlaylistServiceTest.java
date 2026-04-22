package com.fivefy.domain.playlist.service;

import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.playlist.dto.request.PlaylistCreateRequest;
import com.fivefy.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.fivefy.domain.playlist.dto.response.PlaylistDeleteResponse;
import com.fivefy.domain.playlist.dto.response.PlaylistResponse;
import com.fivefy.domain.playlist.entity.Playlist;
import com.fivefy.domain.playlist.enums.PlaylistErrorCode;
import com.fivefy.domain.playlist.repository.PlaylistRepository;
import com.fivefy.domain.subscription.enums.SubscriptionStatus;
import com.fivefy.domain.subscription.repository.SubscriptionRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

    @InjectMocks
    private PlaylistService playlistService;

    @Mock private PlaylistRepository playlistRepository;
    @Mock private SubscriptionRepository subscriptionRepository;

    @Nested
    @DisplayName("플레이리스트 생성")
    class CreatePlaylist {

        @Test
        @DisplayName("플레이리스트 생성 성공")
        void createPlaylistSuccess() {
            // given
            Long userId = 1L;
            PlaylistCreateRequest request = new PlaylistCreateRequest("내 플레이리스트", "설명");

            given(subscriptionRepository.existsByUserIdAndStatusIn(
                    userId,
                    List.of(SubscriptionStatus.FREE, SubscriptionStatus.ACTIVE)
            )).willReturn(true);

            Playlist playlist = Playlist.create(userId, request.title(), request.description());
            ReflectionTestUtils.setField(playlist, "id", 1L);

            given(playlistRepository.existsByUserIdAndTitleAndDeletedFalse(userId, request.title()))
                    .willReturn(false);
            given(playlistRepository.save(any(Playlist.class))).willReturn(playlist);

            // when
            PlaylistResponse result = playlistService.createPlaylist(userId, request);

            // then
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.title()).isEqualTo("내 플레이리스트");
            assertThat(result.description()).isEqualTo("설명");

            verify(playlistRepository, times(1))
                    .existsByUserIdAndTitleAndDeletedFalse(userId, request.title());
            verify(playlistRepository, times(1)).save(any(Playlist.class));
        }

        @Test
        @DisplayName("중복 제목이면 예외 발생")
        void createPlaylistDuplicateName() {
            // given
            Long userId = 1L;
            PlaylistCreateRequest request = new PlaylistCreateRequest("중복 제목", "설명");

            given(subscriptionRepository.existsByUserIdAndStatusIn(
                    userId,
                    List.of(SubscriptionStatus.FREE, SubscriptionStatus.ACTIVE)
            )).willReturn(true);

            given(playlistRepository.existsByUserIdAndTitleAndDeletedFalse(userId, request.title()))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> playlistService.createPlaylist(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.DUPLICATE_PLAYLIST_NAME.getMessage());

            verify(playlistRepository, never()).save(any());
        }

        @Test
        @DisplayName("유효한 구독이 없으면 플레이리스트 생성 시 예외 발생")
        void createPlaylistWithoutValidSubscription() {
            // given
            Long userId = 1L;
            PlaylistCreateRequest request = new PlaylistCreateRequest("내 플레이리스트", "설명");

            given(subscriptionRepository.existsByUserIdAndStatusIn(
                    userId,
                    List.of(SubscriptionStatus.FREE, SubscriptionStatus.ACTIVE)
            )).willReturn(false);

            // when & then
            assertThatThrownBy(() -> playlistService.createPlaylist(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.PLAYLIST_CREATION_SUBSCRIPTION_REQUIRED.getMessage());

            verify(playlistRepository, never())
                    .existsByUserIdAndTitleAndDeletedFalse(anyLong(), anyString());
            verify(playlistRepository, never()).save(any());
        }

        @Test
        @DisplayName("플레이리스트 저장 중 unique 제약 위반 발생 시 중복 제목 예외로 변환")
        void createPlaylist_uniqueConstraintViolation() {
            // given
            Long userId = 1L;
            PlaylistCreateRequest request = new PlaylistCreateRequest("중복 제목", "설명");

            given(subscriptionRepository.existsByUserIdAndStatusIn(
                    userId,
                    List.of(SubscriptionStatus.FREE, SubscriptionStatus.ACTIVE)
            )).willReturn(true);

            given(playlistRepository.existsByUserIdAndTitleAndDeletedFalse(userId, request.title()))
                    .willReturn(false);

            given(playlistRepository.save(any(Playlist.class)))
                    .willThrow(new DataIntegrityViolationException(
                            "could not execute statement; constraint [uk_playlist_user_title_deleted]"
                    ));

            // when & then
            assertThatThrownBy(() -> playlistService.createPlaylist(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.DUPLICATE_PLAYLIST_NAME.getMessage());
        }

        @Test
        @DisplayName("플레이리스트 저장 중 다른 무결성 위반 발생 시 중복 제목 예외로 변환하지 않음")
        void createPlaylist_otherDataIntegrityViolation() {
            // given
            Long userId = 1L;
            PlaylistCreateRequest request = new PlaylistCreateRequest("정상 제목", "설명");

            given(subscriptionRepository.existsByUserIdAndStatusIn(
                    userId,
                    List.of(SubscriptionStatus.FREE, SubscriptionStatus.ACTIVE)
            )).willReturn(true);

            given(playlistRepository.existsByUserIdAndTitleAndDeletedFalse(userId, request.title()))
                    .willReturn(false);

            given(playlistRepository.save(any(Playlist.class)))
                    .willThrow(new DataIntegrityViolationException(
                            "could not execute statement; not-null property references a null or transient value"
                    ));

            // when & then
            assertThatThrownBy(() -> playlistService.createPlaylist(userId, request))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("rootCause 메시지에 중복 제약조건이 포함되면 중복 제목 예외로 변환한다")
        void createPlaylist_duplicateTitle_whenRootCauseContainsConstraint() {
            // given
            Long userId = 1L;
            PlaylistCreateRequest request = new PlaylistCreateRequest("내 플레이리스트", "설명");

            given(subscriptionRepository.existsByUserIdAndStatusIn(
                    userId,
                    List.of(SubscriptionStatus.FREE, SubscriptionStatus.ACTIVE)
            )).willReturn(true);

            given(playlistRepository.existsByUserIdAndTitleAndDeletedFalse(userId, request.title()))
                    .willReturn(false);

            DataIntegrityViolationException exception = mock(DataIntegrityViolationException.class);
            Throwable rootCause = mock(Throwable.class);

            given(exception.getMostSpecificCause()).willReturn(rootCause);
            given(rootCause.getMessage()).willReturn("constraint [uk_playlist_user_title_deleted]");

            given(playlistRepository.save(any(Playlist.class))).willThrow(exception);

            // when & then
            assertThatThrownBy(() -> playlistService.createPlaylist(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.DUPLICATE_PLAYLIST_NAME.getMessage());
        }

        @Test
        @DisplayName("rootCause 메시지가 없으면 예외 메시지로 중복 여부를 판단한다")
        void createPlaylist_duplicateTitle_whenRootCauseMessageIsNull() {
            // given
            Long userId = 1L;
            PlaylistCreateRequest request = new PlaylistCreateRequest("내 플레이리스트", "설명");

            given(subscriptionRepository.existsByUserIdAndStatusIn(
                    userId,
                    List.of(SubscriptionStatus.FREE, SubscriptionStatus.ACTIVE)
            )).willReturn(true);

            given(playlistRepository.existsByUserIdAndTitleAndDeletedFalse(userId, request.title()))
                    .willReturn(false);

            DataIntegrityViolationException exception = mock(DataIntegrityViolationException.class);
            Throwable rootCause = mock(Throwable.class);

            given(exception.getMostSpecificCause()).willReturn(rootCause);
            given(rootCause.getMessage()).willReturn(null);
            given(exception.getMessage()).willReturn("constraint [uk_playlist_user_title_deleted]");

            given(playlistRepository.save(any(Playlist.class))).willThrow(exception);

            // when & then
            assertThatThrownBy(() -> playlistService.createPlaylist(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.DUPLICATE_PLAYLIST_NAME.getMessage());
        }

        @Test
        @DisplayName("rootCause가 없으면 예외 메시지로 중복 여부를 판단한다")
        void createPlaylist_duplicateTitle_whenRootCauseIsNull() {
            // given
            Long userId = 1L;
            PlaylistCreateRequest request = new PlaylistCreateRequest("내 플레이리스트", "설명");

            given(subscriptionRepository.existsByUserIdAndStatusIn(
                    userId,
                    List.of(SubscriptionStatus.FREE, SubscriptionStatus.ACTIVE)
            )).willReturn(true);

            given(playlistRepository.existsByUserIdAndTitleAndDeletedFalse(userId, request.title()))
                    .willReturn(false);

            DataIntegrityViolationException exception = mock(DataIntegrityViolationException.class);

            given(exception.getMostSpecificCause()).willReturn(null);
            given(exception.getMessage()).willReturn("constraint [uk_playlist_user_title_deleted]");

            given(playlistRepository.save(any(Playlist.class))).willThrow(exception);

            // when & then
            assertThatThrownBy(() -> playlistService.createPlaylist(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.DUPLICATE_PLAYLIST_NAME.getMessage());
        }

        @Test
        @DisplayName("중복 제약조건이 아니면 duplicate 예외로 변환하지 않는다")
        void createPlaylist_dataIntegrityViolation_notDuplicateConstraint() {
            // given
            Long userId = 1L;
            PlaylistCreateRequest request = new PlaylistCreateRequest("내 플레이리스트", "설명");

            given(subscriptionRepository.existsByUserIdAndStatusIn(
                    userId,
                    List.of(SubscriptionStatus.FREE, SubscriptionStatus.ACTIVE)
            )).willReturn(true);

            given(playlistRepository.existsByUserIdAndTitleAndDeletedFalse(userId, request.title()))
                    .willReturn(false);

            DataIntegrityViolationException exception = mock(DataIntegrityViolationException.class);
            Throwable rootCause = mock(Throwable.class);

            given(exception.getMostSpecificCause()).willReturn(rootCause);
            given(rootCause.getMessage()).willReturn(null);
            given(exception.getMessage()).willReturn("some other integrity violation");

            given(playlistRepository.save(any(Playlist.class))).willThrow(exception);

            // when & then
            assertThatThrownBy(() -> playlistService.createPlaylist(userId, request))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("예외 메시지가 없으면 duplicate 예외로 변환하지 않는다")
        void createPlaylist_dataIntegrityViolation_whenExceptionMessageIsNull() {
            // given
            Long userId = 1L;
            PlaylistCreateRequest request = new PlaylistCreateRequest("내 플레이리스트", "설명");

            given(subscriptionRepository.existsByUserIdAndStatusIn(
                    userId,
                    List.of(SubscriptionStatus.FREE, SubscriptionStatus.ACTIVE)
            )).willReturn(true);

            given(playlistRepository.existsByUserIdAndTitleAndDeletedFalse(userId, request.title()))
                    .willReturn(false);

            DataIntegrityViolationException exception = mock(DataIntegrityViolationException.class);
            Throwable rootCause = mock(Throwable.class);

            given(exception.getMostSpecificCause()).willReturn(rootCause);
            given(rootCause.getMessage()).willReturn(null);
            given(exception.getMessage()).willReturn(null);

            given(playlistRepository.save(any(Playlist.class))).willThrow(exception);

            // when & then
            assertThatThrownBy(() -> playlistService.createPlaylist(userId, request))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("플레이리스트 목록 조회")
    class GetPlaylists {

        @Test
        @DisplayName("플레이리스트 목록 조회 성공")
        void getPlaylistsSuccess() {
            // given
            Pageable pageable = PageRequest.of(0, 20);

            Playlist playlist1 = Playlist.create(1L, "플리1", "설명1");
            Playlist playlist2 = Playlist.create(2L, "플리2", "설명2");

            ReflectionTestUtils.setField(playlist1, "id", 1L);
            ReflectionTestUtils.setField(playlist2, "id", 2L);

            Page<Playlist> page = new PageImpl<>(List.of(playlist1, playlist2), pageable, 2);

            given(playlistRepository.findAllByDeletedFalse(pageable)).willReturn(page);

            // when
            PageResponse<PlaylistResponse> result = playlistService.getPlaylists(pageable);

            // then
            assertThat(result.content()).hasSize(2);
            assertThat(result.page()).isEqualTo(0);
            assertThat(result.size()).isEqualTo(20);
            assertThat(result.totalElements()).isEqualTo(2L);
            assertThat(result.totalPages()).isEqualTo(1);
            assertThat(result.content().get(0).title()).isEqualTo("플리1");
            assertThat(result.content().get(1).title()).isEqualTo("플리2");
        }
    }

    @Nested
    @DisplayName("플레이리스트 단건 조회")
    class GetPlaylist {

        @Test
        @DisplayName("플레이리스트 단건 조회 성공")
        void getPlaylistSuccess() {
            // given
            Long playlistId = 1L;

            Playlist playlist = Playlist.create(1L, "내 플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));

            // when
            PlaylistResponse result = playlistService.getPlaylist(playlistId);

            // then
            assertThat(result.id()).isEqualTo(playlistId);
            assertThat(result.title()).isEqualTo("내 플리");
            assertThat(result.description()).isEqualTo("설명");
        }

        @Test
        @DisplayName("존재하지 않는 플레이리스트 조회 시 예외 발생")
        void getPlaylistNotFound() {
            // given
            Long playlistId = 1L;

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> playlistService.getPlaylist(playlistId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.PLAYLIST_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("플레이리스트 수정")
    class UpdatePlaylist {

        @Test
        @DisplayName("플레이리스트 수정 성공")
        void updatePlaylistSuccess() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            PlaylistUpdateRequest request = new PlaylistUpdateRequest("수정된 제목", "수정된 설명");

            Playlist playlist = Playlist.create(userId, "기존 제목", "기존 설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));
            given(playlistRepository.existsByUserIdAndTitleAndDeletedFalse(userId, request.title()))
                    .willReturn(false);

            // when
            PlaylistResponse result = playlistService.updatePlaylist(userId, playlistId, request);

            // then
            assertThat(result.id()).isEqualTo(playlistId);
            assertThat(result.title()).isEqualTo("수정된 제목");
            assertThat(result.description()).isEqualTo("수정된 설명");
        }

        @Test
        @DisplayName("존재하지 않는 플레이리스트 수정 시 예외 발생")
        void updatePlaylistNotFound() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            PlaylistUpdateRequest request = new PlaylistUpdateRequest("수정 제목", "설명");

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> playlistService.updatePlaylist(userId, playlistId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.PLAYLIST_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("본인 소유가 아닌 플레이리스트 수정 시 예외 발생")
        void updatePlaylistForbidden() {
            // given
            Long userId = 1L;
            Long otherUserId = 2L;
            Long playlistId = 1L;
            PlaylistUpdateRequest request = new PlaylistUpdateRequest("수정 제목", "설명");

            Playlist playlist = Playlist.create(otherUserId, "기존 제목", "기존 설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));

            // when & then
            assertThatThrownBy(() -> playlistService.updatePlaylist(userId, playlistId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.PLAYLIST_UPDATE_FORBIDDEN.getMessage());

            verify(playlistRepository, never())
                    .existsByUserIdAndTitleAndDeletedFalse(anyLong(), anyString());
        }

        @Test
        @DisplayName("제목이 변경되었고 중복 제목이면 예외 발생")
        void updatePlaylistDuplicateName() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            PlaylistUpdateRequest request = new PlaylistUpdateRequest("새 제목", "설명");

            Playlist playlist = Playlist.create(userId, "기존 제목", "기존 설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));
            given(playlistRepository.existsByUserIdAndTitleAndDeletedFalse(userId, request.title()))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> playlistService.updatePlaylist(userId, playlistId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.DUPLICATE_PLAYLIST_NAME.getMessage());
        }

        @Test
        @DisplayName("제목이 변경되지 않으면 중복 체크 없이 수정 성공")
        void updatePlaylistSameTitleSuccess() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;
            PlaylistUpdateRequest request = new PlaylistUpdateRequest("기존 제목", "새 설명");

            Playlist playlist = Playlist.create(userId, "기존 제목", "기존 설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));

            // when
            PlaylistResponse result = playlistService.updatePlaylist(userId, playlistId, request);

            // then
            assertThat(result.title()).isEqualTo("기존 제목");
            assertThat(result.description()).isEqualTo("새 설명");

            verify(playlistRepository, never())
                    .existsByUserIdAndTitleAndDeletedFalse(anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("플레이리스트 삭제")
    class DeletePlaylist {

        @Test
        @DisplayName("플레이리스트 삭제 성공")
        void deletePlaylistSuccess() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;

            Playlist playlist = Playlist.create(userId, "삭제할 플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));

            // when
            PlaylistDeleteResponse result = playlistService.deletePlaylist(userId, playlistId);

            // then
            assertThat(result.id()).isEqualTo(playlistId);
            assertThat(result.deletedAt()).isNotNull();
            assertThat(playlist.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("중복되지 않은 제목이면 플레이리스트 생성 성공")
        void createPlaylist_success_whenTitleNotDuplicated() {
            // given
            Long userId = 1L;
            PlaylistCreateRequest request = new PlaylistCreateRequest("내 플레이리스트", "설명");

            given(subscriptionRepository.existsByUserIdAndStatusIn(
                    userId,
                    List.of(SubscriptionStatus.FREE, SubscriptionStatus.ACTIVE)
            )).willReturn(true);

            given(playlistRepository.existsByUserIdAndTitleAndDeletedFalse(userId, request.title()))
                    .willReturn(false);

            Playlist playlist = Playlist.create(userId, request.title(), request.description());
            ReflectionTestUtils.setField(playlist, "id", 1L);

            given(playlistRepository.save(any(Playlist.class))).willReturn(playlist);

            // when
            PlaylistResponse result = playlistService.createPlaylist(userId, request);

            // then
            assertThat(result.title()).isEqualTo("내 플레이리스트");
        }

        @Test
        @DisplayName("존재하지 않는 플레이리스트 삭제 시 예외 발생")
        void deletePlaylistNotFound() {
            // given
            Long userId = 1L;
            Long playlistId = 1L;

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.empty());


            // when & then
            assertThatThrownBy(() -> playlistService.deletePlaylist(userId, playlistId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.PLAYLIST_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("본인 소유가 아닌 플레이리스트 삭제 시 예외 발생")
        void deletePlaylistForbidden() {
            // given
            Long userId = 1L;
            Long otherUserId = 2L;
            Long playlistId = 1L;

            Playlist playlist = Playlist.create(otherUserId, "남의 플리", "설명");
            ReflectionTestUtils.setField(playlist, "id", playlistId);

            given(playlistRepository.findByIdAndDeletedFalse(playlistId))
                    .willReturn(Optional.of(playlist));

            // when & then
            assertThatThrownBy(() -> playlistService.deletePlaylist(userId, playlistId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaylistErrorCode.PLAYLIST_DELETE_FORBIDDEN.getMessage());
        }
    }
}
