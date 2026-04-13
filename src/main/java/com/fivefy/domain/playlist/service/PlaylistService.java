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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistService {

    private final PlaylistRepository playlistRepository;

    @Transactional
    public PlaylistResponse createPlaylist(Long userId, PlaylistCreateRequest request) {
        if (playlistRepository.existsByUserIdAndTitleAndDeletedAtIsNull(userId, request.title())) {
            throw new BusinessException(PlaylistErrorCode.DUPLICATE_PLAYLIST_NAME);
        }

        Playlist playlist = Playlist.create(
                userId,
                request.title(),
                request.description()
        );

        Playlist savedPlaylist = playlistRepository.save(playlist);

        return PlaylistResponse.from(savedPlaylist);
    }

    public PageResponse<PlaylistResponse> getPlaylists(Pageable pageable) {
        Page<PlaylistResponse> page = playlistRepository.findAllByDeletedAtIsNull(pageable)
                .map(PlaylistResponse::from);

        return PageResponse.from(page);
    }

    public PlaylistResponse getPlaylist(Long playlistId) {
        Playlist playlist = playlistRepository.findByIdAndDeletedAtIsNull(playlistId)
                .orElseThrow(() -> new BusinessException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

        return PlaylistResponse.from(playlist);
    }

    @Transactional
    public PlaylistResponse updatePlaylist(Long userId, Long playlistId, PlaylistUpdateRequest request) {
        Playlist playlist = playlistRepository.findByIdAndDeletedAtIsNull(playlistId)
                .orElseThrow(() -> new BusinessException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

        if (!playlist.isOwner(userId)) {
            throw new BusinessException(PlaylistErrorCode.PLAYLIST_UPDATE_FORBIDDEN);
        }

        playlist.update(request.title(), request.description());

        return PlaylistResponse.from(playlist);
    }

    @Transactional
    public PlaylistDeleteResponse deletePlaylist(Long userId, Long playlistId) {
        Playlist playlist = playlistRepository.findByIdAndDeletedAtIsNull(playlistId)
                .orElseThrow(() -> new BusinessException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

        if (!playlist.isOwner(userId)) {
            throw new BusinessException(PlaylistErrorCode.PLAYLIST_DELETE_FORBIDDEN);
        }

        playlist.delete();

        return PlaylistDeleteResponse.from(playlist);
    }
}
