package com.fivefy.domain.playlisttrack.service;

import com.fivefy.common.exception.BusinessException;
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
    public void addTrack(Long playlistId, PlaylistTrackCreateRequest request) {
        if (playlistTrackRepository.existsByPlaylistIdAndTrackId(playlistId, request.trackId())) {
            throw new BusinessException(PLAYLIST_TRACK_ALREADY_EXISTS);
        }

        int nextPosition = playlistTrackRepository.countByPlaylistId(playlistId) + 1;

        PlaylistTrack playlistTrack = PlaylistTrack.create(
                playlistId,
                request.trackId(),
                nextPosition
        );

        playlistTrackRepository.save(playlistTrack);
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
                .orElseThrow(() -> new BusinessException(PLAYLIST_TRACK_NOT_FOUND));

        int newPosition = request.position();

        if (newPosition < 1 || newPosition > playlistTracks.size()) {
            throw new BusinessException(INVALID_PLAYLIST_TRACK_POSITION);
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
                .orElseThrow(() -> new BusinessException(PLAYLIST_TRACK_NOT_FOUND));

        playlistTracks.remove(target);
        playlistTrackRepository.delete(target);

        for (int i = 0; i < playlistTracks.size(); i++) {
            playlistTracks.get(i).updatePosition(i + 1);
        }
    }
}
