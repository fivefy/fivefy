package com.fivefy.domain.playlist.service;

import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.playlist.dto.request.PlaylistCreateRequest;
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
}
