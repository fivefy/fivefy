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

    // 알림 수신 동의 팔로워 페이징 조회 (RabbitMQ Consumer 청크 처리용)
    Page<Follow> findAllByArtistIdAndNotificationEnabledTrue(Long artistId, Pageable pageable);
}
