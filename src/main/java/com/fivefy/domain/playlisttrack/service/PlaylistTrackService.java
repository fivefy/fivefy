package com.fivefy.domain.playlisttrack.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.playlist.enums.PlaylistErrorCode;
import com.fivefy.domain.playlisttrack.dto.request.PlaylistTrackCreateRequest;
import com.fivefy.domain.playlisttrack.dto.request.PlaylistTrackOrderUpdateRequest;
import com.fivefy.domain.playlisttrack.dto.response.PlaylistTrackResponse;
import com.fivefy.domain.playlisttrack.entity.PlaylistTrack;
import com.fivefy.domain.playlisttrack.repository.PlaylistTrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.fivefy.domain.playlist.enums.PlaylistErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistTrackService {

    private final PlaylistTrackRepository playlistTrackRepository;

    @Transactional
    public PlaylistTrackResponse addTrack(Long playlistId, PlaylistTrackCreateRequest request) {
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

    public List<PlaylistTrackResponse> getTracks(Long playlistId) {
        return playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(playlistId)
                .stream()
                .map(PlaylistTrackResponse::from)
                .toList();
    }

    @Transactional
    public void updateTrackOrder(Long playlistId, PlaylistTrackOrderUpdateRequest request) {
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

        playlistTracks.remove(target);
        playlistTracks.add(newPosition - 1, target);

        for (int i = 0; i < playlistTracks.size(); i++) {
            playlistTracks.get(i).updatePosition(i + 1);
        }
    }

    @Transactional
    public void deleteTrack(Long playlistId, Long trackId) {
        List<PlaylistTrack> playlistTracks = playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(playlistId);

        PlaylistTrack target = playlistTracks.stream()
                .filter(playlistTrack -> playlistTrack.getTrackId().equals(trackId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(PlaylistErrorCode.PLAYLIST_TRACK_NOT_FOUND));

        playlistTracks.remove(target);
        playlistTrackRepository.delete(target);

        for (int i = 0; i < playlistTracks.size(); i++) {
            playlistTracks.get(i).updatePosition(i + 1);
        }
    }
}
