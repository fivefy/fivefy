package com.fivefy.domain.follow.repository;

import com.fivefy.domain.follow.entity.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByUserIdAndArtistId(Long userId, Long artistId);
    boolean existsByUserIdAndArtistId(Long userId, Long artistId);
    Page<Follow> findAllByUserId(Long userId, Pageable pageable);

    // PUBLISH_TRACK 알림: 알림 수신 동의한 팔로워 목록
    List<Follow> findAllByArtistIdAndNotificationEnabledTrue(Long artistId);
}
