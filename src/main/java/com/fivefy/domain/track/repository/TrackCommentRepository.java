package com.fivefy.domain.track.repository;

import com.fivefy.domain.track.entity.TrackComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackCommentRepository extends JpaRepository<TrackComment, Long>, TrackCommentQueryRepository {
}