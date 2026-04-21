package com.fivefy.domain.album.repository;

import com.fivefy.domain.album.entity.Album;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumRepository extends JpaRepository<Album, Long>, AlbumQueryRepository {
}
