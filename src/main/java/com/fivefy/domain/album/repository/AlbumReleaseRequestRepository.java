package com.fivefy.domain.album.repository;

import com.fivefy.domain.album.entity.AlbumReleaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumReleaseRequestRepository extends JpaRepository<AlbumReleaseRequest, Long>, AlbumReleaseRequestQueryRepository {
}
