package com.fivefy.domain.follow.repository;

import com.fivefy.domain.follow.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByUserIdAndArtistId(Long userId, Long artistId);
    boolean existsByUserIdAndArtistId(Long userId, Long artistId);
    List<Follow> findAllByUserId(Long userId);
}
