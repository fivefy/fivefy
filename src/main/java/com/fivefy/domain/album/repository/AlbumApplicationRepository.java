package com.fivefy.domain.album.repository;

import com.fivefy.domain.album.entity.AlbumApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumApplicationRepository extends JpaRepository<AlbumApplication, Long>, AlbumApplicationQueryRepository {
}
