package com.fivefy.domain.track.repository;

import com.fivefy.domain.track.entity.TrackComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 트랙 댓글 커스텀 리포지토리
 */
public interface TrackCommentQueryRepository {

    /**
     * 트랙 댓글 목록 조회
     */
    Page<TrackComment> getTrackComments(Long trackId, Pageable pageable);
}