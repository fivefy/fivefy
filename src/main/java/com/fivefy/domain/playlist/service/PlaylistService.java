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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistService {

    private static final String PLAYLIST_UNIQUE_CONSTRAINT = "uk_playlist_user_title_deleted";

    private final PlaylistRepository playlistRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public PlaylistResponse createPlaylist(Long userId, PlaylistCreateRequest request) {
        // 플레이리스트 생성 전 구독 상태 검증
        // TRIAL 또는 ACTIVE 상태인 경우에만 생성 가능
        validateSubscription(userId);

        // 중복 제목 여부 검사
        if (playlistRepository.existsByUserIdAndTitleAndDeletedFalse(userId, request.title())) {
            throw new BusinessException(PlaylistErrorCode.DUPLICATE_PLAYLIST_NAME);
        }

        Playlist playlist = Playlist.create(
                userId,
                request.title(),
                request.description()
        );

        try {
            Playlist savedPlaylist = playlistRepository.save(playlist);
            return PlaylistResponse.from(savedPlaylist);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicatePlaylistTitleException(e)) {
                throw new BusinessException(PlaylistErrorCode.DUPLICATE_PLAYLIST_NAME);
            }
            throw e;
        }
    }

    public PageResponse<PlaylistResponse> getPlaylists(Pageable pageable) {
        Page<PlaylistResponse> page = playlistRepository.findAllByDeletedFalse(pageable)
                .map(PlaylistResponse::from);

        return PageResponse.from(page);
    }

    public PlaylistResponse getPlaylist(Long playlistId) {
        Playlist playlist = playlistRepository.findByIdAndDeletedFalse(playlistId)
                .orElseThrow(() -> new BusinessException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

        return PlaylistResponse.from(playlist);
    }

    @Transactional
    public PlaylistResponse updatePlaylist(Long userId, Long playlistId, PlaylistUpdateRequest request) {
        Playlist playlist = playlistRepository.findByIdAndDeletedFalse(playlistId)
                .orElseThrow(() -> new BusinessException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

        // 본인이 생성한 플레이리스트만 수정 가능
        if (!playlist.isOwner(userId)) {
            throw new BusinessException(PlaylistErrorCode.PLAYLIST_UPDATE_FORBIDDEN);
        }

        // 제목이 변경된 경우에만 활성 플레이리스트 기준 중복 체크
        if (!playlist.getTitle().equals(request.title())
                && playlistRepository.existsByUserIdAndTitleAndDeletedFalse(userId, request.title())) {
            throw new BusinessException(PlaylistErrorCode.DUPLICATE_PLAYLIST_NAME);
        }

        playlist.update(request.title(), request.description());

        return PlaylistResponse.from(playlist);
    }

    @Transactional
    public PlaylistDeleteResponse deletePlaylist(Long userId, Long playlistId) {
        Playlist playlist = playlistRepository.findByIdAndDeletedFalse(playlistId)
                .orElseThrow(() -> new BusinessException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

        // 본인이 생성한 플레이리스트만 삭제 가능
        if (!playlist.isOwner(userId)) {
            throw new BusinessException(PlaylistErrorCode.PLAYLIST_DELETE_FORBIDDEN);
        }

        playlist.delete();

        return PlaylistDeleteResponse.from(playlist);
    }

    private boolean isDuplicatePlaylistTitleException(DataIntegrityViolationException e) {
        Throwable rootCause = e.getMostSpecificCause();

        if (rootCause != null && rootCause.getMessage() != null) {
            return rootCause.getMessage().contains(PLAYLIST_UNIQUE_CONSTRAINT);
        }

        return e.getMessage() != null && e.getMessage().contains(PLAYLIST_UNIQUE_CONSTRAINT);
    }

    private void validateSubscription(Long userId) {
        // 사용자 구독 상태 조회
        // FREE(체험), ACTIVE(유료) 상태인 경우 유효한 구독으로 판단
        boolean hasValidSubscription = subscriptionRepository.existsByUserIdAndStatusIn(
                userId,
                List.of(SubscriptionStatus.FREE, SubscriptionStatus.ACTIVE)
        );

        // 유효한 구독이 없으면 플레이리스트 생성 불가
        if (!hasValidSubscription) {
            throw new BusinessException(PlaylistErrorCode.PLAYLIST_CREATION_SUBSCRIPTION_REQUIRED);
        }
    }
}
