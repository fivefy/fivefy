package com.fivefy.domain.playlisttrack.repository;

import com.fivefy.domain.playlisttrack.entity.PlaylistTrack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, Long> {

    List<PlaylistTrack> findAllByPlaylistIdOrderByPositionAsc(Long playlistId);
    Optional<PlaylistTrack> findByPlaylistIdAndTrackId(Long playlistId, Long trackId);
    boolean existsByPlaylistIdAndTrackId(Long playlistId, Long trackId);
    int countByPlaylistId(Long playlistId);
    // 특정 구간의 트랙 조회 (순서 변경 시 영향 범위만 조회하기 위함)
    // ex) 2 -> 4 이동 시, 3~4 위치 트랙만 조회하여 부분 재정렬에 사용
    List<PlaylistTrack> findByPlaylistIdAndPositionBetweenOrderByPositionAsc(Long playlistId, int startPosition, int endPosition);
    // 특정 위치 이후의 트랙 조회 (삭제 후 뒤에 있는 트랙을 앞으로 당기기 위함)
    // ex) 3번 트랙 삭제 시, 4번 이후 트랙들을 조회하여 position -1 처리
    List<PlaylistTrack> findByPlaylistIdAndPositionGreaterThanOrderByPositionAsc(Long playlistId, int position);
}
