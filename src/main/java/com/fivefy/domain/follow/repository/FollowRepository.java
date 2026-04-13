package com.fivefy.domain.follow.repository;

import com.fivefy.domain.follow.entity.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByUserIdAndArtistId(Long userId, Long artistId);
    boolean existsByUserIdAndArtistId(Long userId, Long artistId);
    Page<Follow> findAllByUserId(Long userId, Pageable pageable);
}
