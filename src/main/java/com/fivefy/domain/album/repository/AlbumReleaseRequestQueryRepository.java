package com.fivefy.domain.album.repository;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.album.entity.AlbumReleaseRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * AlbumReleaseRequest Querydsl 전용 Repository
 */
public interface AlbumReleaseRequestQueryRepository {

    boolean existsPendingRequest(Long requesterUserId, Long artistId, String title);

    List<AlbumReleaseRequest> searchMyAlbumReleaseRequests(Long requesterUserId);

    Page<AlbumReleaseRequest> searchAlbumReleaseRequests(
            ApplicationStatus status,
            Pageable pageable
    );
}