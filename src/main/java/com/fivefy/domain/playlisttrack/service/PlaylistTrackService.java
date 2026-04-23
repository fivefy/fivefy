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

        // 현재 플레이리스트의 트랙 목록 조회 (순서 기준)
        List<PlaylistTrack> playlistTracks = playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(playlistId);

        // 이동 대상 트랙 조회
        PlaylistTrack target = playlistTracks.stream()
                .filter(playlistTrack -> playlistTrack.getTrackId().equals(request.trackId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(PlaylistErrorCode.PLAYLIST_TRACK_NOT_FOUND));

        int oldPosition = target.getPosition();
        int newPosition = request.position();

        // 이동할 위치는 1 ~ 트랙 개수 범위여야 함
        if (newPosition < 1 || newPosition > playlistTracks.size()) {
            throw new BusinessException(PlaylistErrorCode.INVALID_PLAYLIST_TRACK_POSITION);
        }

        // 현재 위치와 같으면 변경할 필요 없음
        if (oldPosition == newPosition) {
            return;
        }

        try {
            // 이동 방향에 따라 영향 범위만 부분 재정렬
            if (oldPosition < newPosition) {
                moveDown(playlistId, target, oldPosition, newPosition);
            } else {
                moveUp(playlistId, target, oldPosition, newPosition);
            }
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

        // 삭제 대상 트랙 조회
        PlaylistTrack target = playlistTrackRepository.findByPlaylistIdAndTrackId(playlistId, trackId)
                .orElseThrow(() -> new BusinessException(PlaylistErrorCode.PLAYLIST_TRACK_NOT_FOUND));

        int deletedPosition = target.getPosition();

        // 트랙 삭제
        playlistTrackRepository.delete(target);

        try {
            // 삭제를 먼저 DB에 반영 (position 충돌 방지)
            playlistTrackRepository.flush();

            // 삭제된 위치 이후 트랙만 조회하여 position을 한 칸씩 앞으로 당김
            List<PlaylistTrack> affectedTracks =
                    playlistTrackRepository.findByPlaylistIdAndPositionGreaterThanOrderByPositionAsc(
                            playlistId, deletedPosition
                    );

            for (PlaylistTrack track : affectedTracks) {
                track.updatePosition(track.getPosition() - 1);
            }

            playlistTrackRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw handlePlaylistTrackConstraintException(e);
        }
    }

    /**
     * 아래로 이동 (oldPosition < newPosition)
     * 예: 2 -> 4
     * - target: 2 → 4 이동
     * - 3, 4 위치 트랙들은 각각 1칸씩 앞으로 이동 (3→2, 4→3)
     */
    private void moveDown(Long playlistId, PlaylistTrack target, int oldPosition, int newPosition) {
        List<PlaylistTrack> affectedTracks =
                playlistTrackRepository.findByPlaylistIdAndPositionBetweenOrderByPositionAsc(
                        playlistId, oldPosition + 1, newPosition
                );

        // 충돌 방지를 위해 영향 범위 + target을 임시 음수로 변경
        target.moveToTemporaryPosition(-oldPosition);
        for (PlaylistTrack track : affectedTracks) {
            track.moveToTemporaryPosition(-track.getPosition());
        }

        playlistTrackRepository.flush();

        // affected 트랙: 한 칸씩 앞으로 이동
        for (PlaylistTrack track : affectedTracks) {
            int originalPosition = -track.getPosition();
            track.updatePosition(originalPosition - 1);
        }

        // target: 요청 위치로 이동
        target.updatePosition(newPosition);

        playlistTrackRepository.flush();
    }

    /**
     * 위로 이동
     * 예: 4 -> 2
     * - target: 4 -> 2
     * - 2, 3 위치 트랙들은 각각 1칸씩 뒤로 이동 (2→3, 3→4)
     */
    private void moveUp(Long playlistId, PlaylistTrack target, int oldPosition, int newPosition) {
        List<PlaylistTrack> affectedTracks =
                playlistTrackRepository.findByPlaylistIdAndPositionBetweenOrderByPositionAsc(
                        playlistId, newPosition, oldPosition - 1
                );

        // 충돌 방지를 위해 영향 범위 + target을 임시 음수로 변경
        target.moveToTemporaryPosition(-oldPosition);
        for (PlaylistTrack track : affectedTracks) {
            track.moveToTemporaryPosition(-track.getPosition());
        }

        playlistTrackRepository.flush();

        // affected 트랙: 한 칸씩 뒤로 이동
        for (PlaylistTrack track : affectedTracks) {
            int originalPosition = -track.getPosition();
            track.updatePosition(originalPosition + 1);
        }

        // target: 요청 위치로 이동
        target.updatePosition(newPosition);

        playlistTrackRepository.flush();
    }

    // DB 유니크 제약조건 위반 시 constraint 이름을 기반으로 적절한 비즈니스 예외 변환
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
