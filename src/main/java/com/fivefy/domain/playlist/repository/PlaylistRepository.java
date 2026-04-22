package com.fivefy.domain.playlist.repository;

import com.fivefy.domain.playlist.entity.Playlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    Page<Playlist> findAllByDeletedAtIsNull(Pageable pageable);
    Optional<Playlist> findByIdAndDeletedAtIsNull(Long id);
    boolean existsByUserIdAndTitleAndDeletedAtIsNull(Long userId, String title);

    @Modifying
    @Query("delete from Playlist p where p.deletedAt is not null and p.deletedAt <= :threshold")
    int deleteAllSoftDeletedBefore(@Param("threshold") LocalDateTime threshold);
}
