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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistTrackService {

    // DB 유니크 제약 조건 이름 (예외 발생 시 어떤 충돌인지 구분하기 위해 사용)
    private static final String UK_PLAYLIST_TRACK_PLAYLIST_POSITION = "uk_playlist_track_playlist_position";
    private static final String UK_PLAYLIST_TRACK_PLAYLIST_TRACK = "uk_playlist_track_playlist_track";

    private final PlaylistTrackRepository playlistTrackRepository;
    private final PlaylistRepository playlistRepository;
    private final TrackRepository trackRepository;

    @Transactional
    public PlaylistTrackResponse addTrack(Long userId, Long playlistId, PlaylistTrackCreateRequest request) {
        Playlist playlist = playlistRepository.findByIdAndDeletedFalse(playlistId)
                .orElseThrow(() -> new BusinessException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

        // 본인 플레이리스트만 트랙 추가 가능
        if (!playlist.isOwner(userId)) {
            throw new BusinessException(PlaylistErrorCode.PLAYLIST_ACCESS_FORBIDDEN);
        }

        // 존재하는 트랙인지 검증
        if (!trackRepository.existsById(request.trackId())) {
            throw new BusinessException(PlaylistErrorCode.TRACK_NOT_FOUND);
        }

        // 동일 트랙 중복 추가 방지
        if (playlistTrackRepository.existsByPlaylistIdAndTrackId(playlistId, request.trackId())) {
            throw new BusinessException(PlaylistErrorCode.PLAYLIST_TRACK_ALREADY_EXISTS);
        }

        int nextPosition = playlistTrackRepository.countByPlaylistId(playlistId) + 1;

        PlaylistTrack playlistTrack = PlaylistTrack.create(
                playlistId,
                request.trackId(),
                nextPosition
        );

        try {
            PlaylistTrack savedPlaylistTrack = playlistTrackRepository.saveAndFlush(playlistTrack);
            return PlaylistTrackResponse.from(savedPlaylistTrack);
        // DB 유니크 제약조건 충돌 발생 시,
        // 어떤 제약조건이 깨졌는지(constraint name)를 기준으로 예외 구분
        } catch (DataIntegrityViolationException e) {
            throw handlePlaylistTrackConstraintException(e);
        }
    }

    public List<PlaylistTrackResponse> getTracks(Long userId, Long playlistId) {
        Playlist playlist = playlistRepository.findByIdAndDeletedFalse(playlistId)
                .orElseThrow(() -> new BusinessException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

        // 본인 플레이리스트만 트랙 조회 가능
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
        Playlist playlist = playlistRepository.findByIdAndDeletedFalse(playlistId)
                .orElseThrow(() -> new BusinessException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

        // 본인 플레이리스트만 트랙 순서 변경 가능
        if (!playlist.isOwner(userId)) {
            throw new BusinessException(PlaylistErrorCode.PLAYLIST_UPDATE_FORBIDDEN);
        }

        List<PlaylistTrack> playlistTracks = playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(playlistId);

        PlaylistTrack target = playlistTracks.stream()
                .filter(playlistTrack -> playlistTrack.getTrackId().equals(request.trackId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(PlaylistErrorCode.PLAYLIST_TRACK_NOT_FOUND));

        int newPosition = request.position();

        // 이동할 위치는 1 ~ 트랙 개수 범위여야 함
        if (newPosition < 1 || newPosition > playlistTracks.size()) {
            throw new BusinessException(PlaylistErrorCode.INVALID_PLAYLIST_TRACK_POSITION);
        }

        // 현재 위치와 같으면 변경할 필요 없음
        if (target.getPosition().equals(newPosition)) {
            return;
        }

        // 기존 위치에서 제거한 뒤, 요청한 위치에 다시 넣음
        playlistTracks.remove(target);
        playlistTracks.add(newPosition - 1, target);

        try {
            // 변경된 순서대로 position 재정렬
            reorderPositions(playlistTracks);
        // DB 유니크 제약조건 충돌 발생 시,
        // 어떤 제약조건이 깨졌는지(constraint name)를 기준으로 예외 구분
        } catch (DataIntegrityViolationException e) {
            throw handlePlaylistTrackConstraintException(e);
        }
    }

    @Transactional
    public void deleteTrack(Long userId, Long playlistId, Long trackId) {
        Playlist playlist = playlistRepository.findByIdAndDeletedFalse(playlistId)
                .orElseThrow(() -> new BusinessException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

        // 본인 플레이리스트만 트랙 삭제 가능
        if (!playlist.isOwner(userId)) {
            throw new BusinessException(PlaylistErrorCode.PLAYLIST_DELETE_FORBIDDEN);
        }

        List<PlaylistTrack> playlistTracks = playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(playlistId);

        PlaylistTrack target = playlistTracks.stream()
                .filter(playlistTrack -> playlistTrack.getTrackId().equals(trackId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(PlaylistErrorCode.PLAYLIST_TRACK_NOT_FOUND));

        // 삭제 대상 트랙을 목록과 DB에서 제거
        playlistTracks.remove(target);
        playlistTrackRepository.delete(target);

        try {
            // position 충돌 방지를 위해 삭제를 먼저 DB에 반영
            playlistTrackRepository.flush();
            // 변경된 순서대로 position 재정렬
            reorderPositions(playlistTracks);
        } catch (DataIntegrityViolationException e) {
            throw handlePlaylistTrackConstraintException(e);
        }
    }

    /**
     * position 중복 방지를 위해
     * 모든 값을 음수로 변경 후 flush,
     * 이후 1부터 순서대로 다시 할당
     */
    private void reorderPositions(List<PlaylistTrack> playlistTracks) {
        // 임시로 음수 position 부여 (중복 방지)
        for (int i = 0; i < playlistTracks.size(); i++) {
            playlistTracks.get(i).moveToTemporaryPosition(-(i + 1));
        }

        // 변경된 음수 position을 DB에 먼저 반영
        playlistTrackRepository.flush();

        // position을 1부터 순서대로 다시 부여
        for (int i = 0; i < playlistTracks.size(); i++) {
            playlistTracks.get(i).updatePosition(i + 1);
        }

        // 최종 position 반영
        playlistTrackRepository.flush();
    }

    /**
     * DB 유니크 제약조건 위반 시 constraint 이름을 기반으로
     * 적절한 비즈니스 예외 변환
     */
    private BusinessException handlePlaylistTrackConstraintException(DataIntegrityViolationException e) {
        String constraintName = extractConstraintName(e);

        if (UK_PLAYLIST_TRACK_PLAYLIST_TRACK.equalsIgnoreCase(constraintName)) {
            return new BusinessException(PlaylistErrorCode.PLAYLIST_TRACK_ALREADY_EXISTS);
        }

        if (UK_PLAYLIST_TRACK_PLAYLIST_POSITION.equalsIgnoreCase(constraintName)) {
            return new BusinessException(PlaylistErrorCode.PLAYLIST_TRACK_POSITION_CONFLICT);
        }

        throw e;
    }

    // constraint 이름을 추출하여 어떤 제약 조건이 깨졌는지 확인
    private String extractConstraintName(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolationException) {
                return constraintViolationException.getConstraintName();
            }
            current = current.getCause();
        }

        return null;
    }
}
