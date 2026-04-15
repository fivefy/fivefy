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
        // 플레이리스트에 포함된 트랙인지 검증
        validatePlaylistTrackRelation(request.playlistId(), request.trackId());

        // 현재 세션에서 재생 중인 playback 조회 (중복 재생 방지)
        Playback currentPlayback = playbackRepository
                .findTopByUserIdAndSessionIdAndStatusOrderByIdDesc(
                        userId,
                        request.sessionId(),
                        PlaybackStatus.PLAYING
                )
                .orElse(null);

        if (currentPlayback != null) {
            // 같은 곡이면 기존 상태 흐름 처리
            if (currentPlayback.getPlaylistId().equals(request.playlistId())
                    && currentPlayback.getTrackId().equals(request.trackId())) {
                Playback handledPlayback = handlePlay(currentPlayback, request);
                Playback savedPlayback = playbackRepository.save(handledPlayback);
                return PlaybackResponse.from(savedPlayback);
            }

            // 다른 곡이면 기존 재생 종료
            currentPlayback.stop();
        }

        // 기존 이력 재사용 또는 새로운 playback 생성
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

        // 재생 중이 아닐 경우 예외
        if (!playback.isPlaying()) {
            throw new BusinessException(PlaybackErrorCode.CURRENT_PLAYBACK_NOT_FOUND);
        }

        playback.pause();
        return PlaybackResponse.from(playback);
    }

    @Transactional
    public PlaybackResponse stop(Long userId, PlaybackStopRequest request) {
        Playback playback = getOwnedPlayback(userId, request.id());

        // 재생/일시정지 상태가 아니면 예외
        if (!playback.isPlaying() && !playback.isPaused()) {
            throw new BusinessException(PlaybackErrorCode.INVALID_PLAYBACK_STATE);
        }

        playback.stop();
        return PlaybackResponse.from(playback);
    }

    @Transactional
    public PlaybackResponse skip(Long userId, PlaybackSkipRequest request) {
        Playback currentPlayback = getOwnedPlayback(userId, request.id());

        // 재생/일시정지 상태가 아니면 예외
        if (!currentPlayback.isPlaying() && !currentPlayback.isPaused()) {
            throw new BusinessException(PlaybackErrorCode.INVALID_PLAYBACK_STATE);
        }

        // 플레이리스트 트랙 목록 조회
        List<PlaylistTrack> playlistTracks =
                playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(currentPlayback.getPlaylistId());

        if (playlistTracks.isEmpty()) {
            throw new BusinessException(PlaybackErrorCode.PLAYLIST_TRACK_NOT_FOUND);
        }

        // 다음 트랙 계산
        Long nextTrackId = resolveNextTrackId(playlistTracks, currentPlayback.getTrackId());

        // 현재 곡 skip 처리
        currentPlayback.skip();

        // 다음 곡 자동 재생
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
        // 이미 재생 중이면 예외
        if (playback.isPlaying()) {
            throw new BusinessException(PlaybackErrorCode.INVALID_PLAYBACK_STATE);
        }

        // 일시정지 상태 후 재개
        if (playback.isPaused()) {
            playback.resume();
            return playback;
        }

        // 종료/완료/스킵 상태 후 새 playback 생성
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
        // 내 playback만 조회 (존재/권한 구분 없이 NOT_FOUND 처리)
        return playbackRepository.findByIdAndUserId(playbackId, userId)
                .orElseThrow(() -> new BusinessException(PlaybackErrorCode.PLAYBACK_NOT_FOUND));
    }

    private Long resolveNextTrackId(List<PlaylistTrack> playlistTracks, Long currentTrackId) {
        // 현재 트랙 기준으로 다음 트랙 반환 (마지막이면 처음으로 순환)
        for (int i = 0; i < playlistTracks.size(); i++) {
            if (playlistTracks.get(i).getTrackId().equals(currentTrackId)) {
                int nextIndex = (i + 1) % playlistTracks.size();
                return playlistTracks.get(nextIndex).getTrackId();
            }
        }

        throw new BusinessException(PlaybackErrorCode.PLAYBACK_TRACK_MISMATCH);
    }

    private void validatePlaylistTrackRelation(Long playlistId, Long trackId) {
        // 플레이리스트에 포함되지 않은 트랙 요청 방지
        if (!playlistTrackRepository.existsByPlaylistIdAndTrackId(playlistId, trackId)) {
            throw new BusinessException(PlaybackErrorCode.PLAYLIST_TRACK_NOT_INCLUDED);
        }
    }
}
