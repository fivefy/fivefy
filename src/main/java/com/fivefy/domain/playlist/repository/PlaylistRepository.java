package com.fivefy.domain.playlist.repository;

import com.fivefy.domain.playlist.entity.Playlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    Page<Playlist> findAllByDeletedAtIsNull(Pageable pageable);
    Optional<Playlist> findByIdAndDeletedAtIsNull(Long id);
    boolean existsByUserIdAndTitleAndDeletedAtIsNull(Long userId, String title);
}
