package com.fivefy.domain.album.repository;

import com.fivefy.domain.album.entity.AlbumReleaseRequest;

import java.util.List;

/**
 * AlbumReleaseRequest Querydsl 전용 Repository
 */
public interface AlbumReleaseRequestQueryRepository {

    boolean existsPendingRequest(Long requesterUserId, Long artistId, String title);

    List<AlbumReleaseRequest> searchMyAlbumReleaseRequests(Long requesterUserId);
}