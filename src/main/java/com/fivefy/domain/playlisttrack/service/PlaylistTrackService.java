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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistTrackService {

    private final PlaylistTrackRepository playlistTrackRepository;
    private final PlaylistRepository playlistRepository;

    @Transactional
    public PlaylistTrackResponse addTrack(Long userId, Long playlistId, PlaylistTrackCreateRequest request) {
        Playlist playlist = playlistRepository.findByIdAndDeletedAtIsNull(playlistId)
                .orElseThrow(() -> new BusinessException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

        // 본인이 생성한 플레이리스트만 트랙 추가 가능
        if (!playlist.isOwner(userId)) {
            throw new BusinessException(PlaylistErrorCode.PLAYLIST_ACCESS_FORBIDDEN);
        }

        // 동일한 플레이리스트에 같은 트랙이 이미 존재하는지 검사
        if (playlistTrackRepository.existsByPlaylistIdAndTrackId(playlistId, request.trackId())) {
            throw new BusinessException(PlaylistErrorCode.PLAYLIST_TRACK_ALREADY_EXISTS);
        }

        int nextPosition = playlistTrackRepository.countByPlaylistId(playlistId) + 1;

        PlaylistTrack playlistTrack = PlaylistTrack.create(
                playlistId,
                request.trackId(),
                nextPosition
        );

        PlaylistTrack savedPlaylistTrack = playlistTrackRepository.save(playlistTrack);

        return PlaylistTrackResponse.from(savedPlaylistTrack);
    }

    public List<PlaylistTrackResponse> getTracks(Long userId, Long playlistId) {
        Playlist playlist = playlistRepository.findByIdAndDeletedAtIsNull(playlistId)
                .orElseThrow(() -> new BusinessException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

        // 본인이 생성한 플레이리스트만 트랙 조회 가능
        if (!playlist.isOwner(userId)) {
            throw new BusinessException(PlaylistErrorCode.PLAYLIST_ACCESS_FORBIDDEN);
        }

        return playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(playlistId)
                .stream()
                .map(PlaylistTrackResponse::from)
                .toList();
    }

    @Transactional
    public void updateTrackOrder(Long userId, Long playlistId, PlaylistTrackOrderUpdateRequest request) {
        Playlist playlist = playlistRepository.findByIdAndDeletedAtIsNull(playlistId)
                .orElseThrow(() -> new BusinessException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

        // 본인이 생성한 플레이리스트만 순서 변경 가능
        if (!playlist.isOwner(userId)) {
            throw new BusinessException(PlaylistErrorCode.PLAYLIST_UPDATE_FORBIDDEN);
        }

        List<PlaylistTrack> playlistTracks = playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(playlistId);

        PlaylistTrack target = playlistTracks.stream()
                .filter(playlistTrack -> playlistTrack.getTrackId().equals(request.trackId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(PlaylistErrorCode.PLAYLIST_TRACK_NOT_FOUND));

        int newPosition = request.position();

        // 변경할 순서가 유효한 범위인지 검사
        if (newPosition < 1 || newPosition > playlistTracks.size()) {
            throw new BusinessException(PlaylistErrorCode.INVALID_PLAYLIST_TRACK_POSITION);
        }

        // 현재 위치와 동일하면 그대로 종료
        if (target.getPosition().equals(newPosition)) {
            return;
        }

        playlistTracks.remove(target);
        playlistTracks.add(newPosition - 1, target);

        reorderPositions(playlistTracks);
    }

    @Transactional
    public void deleteTrack(Long userId, Long playlistId, Long trackId) {
        Playlist playlist = playlistRepository.findByIdAndDeletedAtIsNull(playlistId)
                .orElseThrow(() -> new BusinessException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

        // 본인이 생성한 플레이리스트만 삭제 가능
        if (!playlist.isOwner(userId)) {
            throw new BusinessException(PlaylistErrorCode.PLAYLIST_DELETE_FORBIDDEN);
        }

        List<PlaylistTrack> playlistTracks = playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(playlistId);

        PlaylistTrack target = playlistTracks.stream()
                .filter(playlistTrack -> playlistTrack.getTrackId().equals(trackId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(PlaylistErrorCode.PLAYLIST_TRACK_NOT_FOUND));

        playlistTracks.remove(target);
        playlistTrackRepository.delete(target);
        playlistTrackRepository.flush(); // 삭제 먼저 DB에 반영

        reorderPositions(playlistTracks);
    }

    private void reorderPositions(List<PlaylistTrack> playlistTracks) {
        for (int i = 0; i < playlistTracks.size(); i++) {
            playlistTracks.get(i).moveToTemporaryPosition(-(i + 1));
        }

        playlistTrackRepository.flush(); // 전부 -1로 먼저 반영

        for (int i = 0; i < playlistTracks.size(); i++) {
            playlistTracks.get(i).updatePosition(i + 1);
        }
    }
}
