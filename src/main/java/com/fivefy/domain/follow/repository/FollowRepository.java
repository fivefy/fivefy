package com.fivefy.domain.follow.repository;

import com.fivefy.domain.follow.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByUserIdAndArtistId(Long userId, Long artistId);
    List<Follow> findAllByUserId(Long userId);
}
