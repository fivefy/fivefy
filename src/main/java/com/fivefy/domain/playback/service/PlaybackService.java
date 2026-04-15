package com.fivefy.domain.playback.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.playback.dto.request.PlaybackPauseRequest;
import com.fivefy.domain.playback.dto.request.PlaybackPlayRequest;
import com.fivefy.domain.playback.dto.request.PlaybackSkipRequest;
import com.fivefy.domain.playback.dto.request.PlaybackStopRequest;
import com.fivefy.domain.playback.dto.response.PlaybackResponse;
import com.fivefy.domain.playback.entity.Playback;
import com.fivefy.domain.playback.enums.PlaybackErrorCode;
import com.fivefy.domain.playback.enums.PlaybackStatus;
import com.fivefy.domain.playback.repository.PlaybackRepository;
import com.fivefy.domain.playlisttrack.entity.PlaylistTrack;
import com.fivefy.domain.playlisttrack.repository.PlaylistTrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaybackService {

    private final PlaybackRepository playbackRepository;
    private final PlaylistTrackRepository playlistTrackRepository;

    @Transactional
    public PlaybackResponse play(Long userId, PlaybackPlayRequest request) {
        validatePlaylistTrackRelation(request.playlistId(), request.trackId());

        Playback currentPlayback = playbackRepository
                .findTopByUserIdAndSessionIdAndStatusOrderByIdDesc(
                        userId,
                        request.sessionId(),
                        PlaybackStatus.PLAYING
                )
                .orElse(null);

        if (currentPlayback != null) {
            if (currentPlayback.getPlaylistId().equals(request.playlistId())
                    && currentPlayback.getTrackId().equals(request.trackId())) {
                Playback handledPlayback = handlePlay(currentPlayback, request);
                Playback savedPlayback = playbackRepository.save(handledPlayback);
                return PlaybackResponse.from(savedPlayback);
            }

            currentPlayback.stop();
        }

        Playback playback = playbackRepository
                .findTopByUserIdAndPlaylistIdAndTrackIdAndSessionIdOrderByIdDesc(
                        userId,
                        request.playlistId(),
                        request.trackId(),
                        request.sessionId()
                )
                .filter(existing -> !existing.isPlaying())
                .map(existing -> handlePlay(existing, request))
                .orElseGet(() -> Playback.create(
                        request.playlistId(),
                        request.trackId(),
                        userId,
                        request.sessionId(),
                        request.deviceId()
                ));

        Playback savedPlayback = playbackRepository.save(playback);
        return PlaybackResponse.from(savedPlayback);
    }

    @Transactional
    public PlaybackResponse pause(Long userId, PlaybackPauseRequest request) {
        Playback playback = getOwnedPlayback(userId, request.id());

        if (!playback.isPlaying()) {
            throw new BusinessException(PlaybackErrorCode.CURRENT_PLAYBACK_NOT_FOUND);
        }

        playback.pause();
        return PlaybackResponse.from(playback);
    }

    @Transactional
    public PlaybackResponse stop(Long userId, PlaybackStopRequest request) {
        Playback playback = getOwnedPlayback(userId, request.id());

        if (!playback.isPlaying() && !playback.isPaused()) {
            throw new BusinessException(PlaybackErrorCode.INVALID_PLAYBACK_STATE);
        }

        playback.stop();
        return PlaybackResponse.from(playback);
    }

    @Transactional
    public PlaybackResponse skip(Long userId, PlaybackSkipRequest request) {
        Playback currentPlayback = getOwnedPlayback(userId, request.id());

        if (!currentPlayback.isPlaying() && !currentPlayback.isPaused()) {
            throw new BusinessException(PlaybackErrorCode.INVALID_PLAYBACK_STATE);
        }

        List<PlaylistTrack> playlistTracks =
                playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(currentPlayback.getPlaylistId());

        if (playlistTracks.isEmpty()) {
            throw new BusinessException(PlaybackErrorCode.PLAYLIST_TRACK_NOT_FOUND);
        }

        Long nextTrackId = resolveNextTrackId(playlistTracks, currentPlayback.getTrackId());

        currentPlayback.skip();

        Playback nextPlayback = Playback.create(
                currentPlayback.getPlaylistId(),
                nextTrackId,
                userId,
                currentPlayback.getSessionId(),
                currentPlayback.getDeviceId()
        );

        Playback savedNextPlayback = playbackRepository.save(nextPlayback);
        return PlaybackResponse.from(savedNextPlayback);
    }

    public List<PlaybackResponse> getPlaybackHistory(Long userId) {
        return playbackRepository.findAllByUserIdOrderByIdDesc(userId).stream()
                .map(PlaybackResponse::from)
                .toList();
    }

    private Playback handlePlay(Playback playback, PlaybackPlayRequest request) {
        if (playback.isPlaying()) {
            throw new BusinessException(PlaybackErrorCode.INVALID_PLAYBACK_STATE);
        }

        if (playback.isPaused()) {
            playback.resume();
            return playback;
        }

        if (playback.isStopped() || playback.isCompleted() || playback.isSkipped()) {
            return Playback.create(
                    request.playlistId(),
                    request.trackId(),
                    playback.getUserId(),
                    request.sessionId(),
                    request.deviceId()
            );
        }

        throw new BusinessException(PlaybackErrorCode.INVALID_PLAYBACK_STATE);
    }

    private Playback getOwnedPlayback(Long userId, Long playbackId) {
        Playback playback = playbackRepository.findById(playbackId)
                .orElseThrow(() -> new BusinessException(PlaybackErrorCode.PLAYBACK_NOT_FOUND));

        if (!playback.isOwner(userId)) {
            throw new BusinessException(PlaybackErrorCode.PLAYBACK_ACCESS_DENIED);
        }

        return playback;
    }

    private Long resolveNextTrackId(List<PlaylistTrack> playlistTracks, Long currentTrackId) {
        for (int i = 0; i < playlistTracks.size(); i++) {
            if (playlistTracks.get(i).getTrackId().equals(currentTrackId)) {
                int nextIndex = (i + 1) % playlistTracks.size();
                return playlistTracks.get(nextIndex).getTrackId();
            }
        }

        throw new BusinessException(PlaybackErrorCode.PLAYBACK_TRACK_MISMATCH);
    }

    private void validatePlaylistTrackRelation(Long playlistId, Long trackId) {
        if (!playlistTrackRepository.existsByPlaylistIdAndTrackId(playlistId, trackId)) {
            throw new BusinessException(PlaybackErrorCode.PLAYLIST_TRACK_NOT_INCLUDED);
        }
    }
}
